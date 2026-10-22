package com.vorzak.security;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class HardenedDownloader {

    public static final int MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    public static final int CONNECT_TIMEOUT_MS = 5000;
    public static final int READ_TIMEOUT_MS = 5000;
    public static final int MAX_REDIRECTS = 3;
    public static final String USER_AGENT = "VORZAK-Provenance-Investigator/1.0";

    public record DownloadResult(byte[] data, String contentType, String sha256Hex) {}

    public DownloadResult download(String urlString) throws IOException {
        String currentUrl = urlString;
        int redirectCount = 0;
        Set<String> visited = new HashSet<>();

        while (true) {
            if (redirectCount > MAX_REDIRECTS) {
                throw new SecurityException("Too many redirects (max: " + MAX_REDIRECTS + ")");
            }

            URI uri;
            try {
                uri = URI.create(currentUrl).normalize();
            } catch (Exception e) {
                throw new SecurityException("Invalid URL syntax: " + currentUrl);
            }

            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new SecurityException("Forbidden scheme: only http and https are allowed");
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new SecurityException("URL must contain a valid hostname");
            }

            if (!visited.add(currentUrl)) {
                throw new SecurityException("Redirect loop detected for: " + currentUrl);
            }

            // Resolve DNS and strictly validate every IP against SSRF rules
            validateHostSecurity(host);

            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            conn.setInstanceFollowRedirects(false); // We validate redirects manually
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "image/png,image/jpeg,image/webp,*/*");
            conn.setUseCaches(false);

            int responseCode = conn.getResponseCode();

            // Handle HTTP Redirects (301, 302, 303, 307, 308)
            if (responseCode >= 300 && responseCode < 400) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isBlank()) {
                    throw new SecurityException("Redirect response missing Location header");
                }
                // Resolve relative redirect against base URL
                currentUrl = uri.resolve(location).toString();
                redirectCount++;
                continue;
            }

            if (responseCode != 200) {
                conn.disconnect();
                throw new IOException("HTTP error code: " + responseCode + " while fetching " + currentUrl);
            }

            String contentType = conn.getContentType();
            long contentLength = conn.getContentLengthLong();
            if (contentLength > MAX_FILE_SIZE_BYTES) {
                conn.disconnect();
                throw new SecurityException("Response content-length exceeds 10MB limit: " + contentLength);
            }

            // Read payload with bounded buffer cap
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream in = conn.getInputStream()) {
                byte[] buffer = new byte[8192];
                int totalRead = 0;
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    totalRead += bytesRead;
                    if (totalRead > MAX_FILE_SIZE_BYTES) {
                        throw new SecurityException("Response body exceeded 10MB limit during download");
                    }
                    baos.write(buffer, 0, bytesRead);
                }
            } finally {
                conn.disconnect();
            }

            byte[] data = baos.toByteArray();
            if (data.length == 0) {
                throw new IOException("Downloaded empty payload from: " + currentUrl);
            }

            // Verify Magic Bytes
            verifyImageMagicBytes(data);

            // Compute SHA-256
            String sha256 = calculateSha256(data);

            return new DownloadResult(data, contentType != null ? contentType : "application/octet-stream", sha256);
        }
    }

    public static void validateHostSecurity(String host) throws SecurityException {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new SecurityException("DNS resolution failed for host: " + host);
        }

        if (addresses == null || addresses.length == 0) {
            throw new SecurityException("Host resolved to no IP addresses: " + host);
        }

        for (InetAddress addr : addresses) {
            validateIpAddress(addr);
        }
    }

    public static void validateIpAddress(InetAddress addr) throws SecurityException {
        // Check for IPv4-mapped IPv6 addresses (e.g. ::ffff:127.0.0.1 or ::ffff:169.254.169.254)
        if (addr instanceof Inet6Address ip6) {
            if (ip6.isIPv4CompatibleAddress()) {
                throw new SecurityException("SSRF Block: IPv4-compatible IPv6 address rejected");
            }
            byte[] bytes = ip6.getAddress();
            boolean isMapped = true;
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) isMapped = false;
            }
            if (bytes[10] != (byte) 0xFF || bytes[11] != (byte) 0xFF) isMapped = false;
            if (isMapped) {
                byte[] v4bytes = Arrays.copyOfRange(bytes, 12, 16);
                try {
                    validateIpAddress(InetAddress.getByAddress(v4bytes));
                    return;
                } catch (UnknownHostException e) {
                    throw new SecurityException("SSRF Block: Malformed IPv4-mapped IPv6 address");
                }
            }
        }

        if (addr.isAnyLocalAddress()) {
            throw new SecurityException("SSRF Block: Any-local address rejected (" + addr.getHostAddress() + ")");
        }
        if (addr.isLoopbackAddress()) {
            throw new SecurityException("SSRF Block: Loopback address rejected (" + addr.getHostAddress() + ")");
        }
        if (addr.isSiteLocalAddress()) {
            throw new SecurityException("SSRF Block: Private/Site-local address rejected (" + addr.getHostAddress() + ")");
        }
        if (addr.isLinkLocalAddress()) {
            throw new SecurityException("SSRF Block: Link-local address rejected (" + addr.getHostAddress() + ")");
        }

        byte[] raw = addr.getAddress();

        // Check IPv4 ranges
        if (raw.length == 4) {
            int b0 = raw[0] & 0xFF;
            int b1 = raw[1] & 0xFF;

            // 127.0.0.0/8 (Loopback)
            if (b0 == 127) throw new SecurityException("SSRF Block: Loopback range 127.x rejected");
            // 10.0.0.0/8 (Private)
            if (b0 == 10) throw new SecurityException("SSRF Block: Private range 10.x rejected");
            // 172.16.0.0/12 (Private)
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) throw new SecurityException("SSRF Block: Private range 172.16-31.x rejected");
            // 192.168.0.0/16 (Private)
            if (b0 == 192 && b1 == 168) throw new SecurityException("SSRF Block: Private range 192.168.x rejected");
            // 169.254.0.0/16 (Link-local & AWS/GCP/Azure Cloud Metadata 169.254.169.254)
            if (b0 == 169 && b1 == 254) throw new SecurityException("SSRF Block: Cloud metadata / Link-local 169.254.x rejected");
            // 0.0.0.0/8
            if (b0 == 0) throw new SecurityException("SSRF Block: Current network 0.0.0.0/8 rejected");
            // 100.64.0.0/10 (Carrier-grade NAT)
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) throw new SecurityException("SSRF Block: Carrier-grade NAT rejected");
        } else if (raw.length == 16) {
            // IPv6 Unique Local Address (fc00::/7)
            if ((raw[0] & 0xFE) == 0xFC) {
                throw new SecurityException("SSRF Block: IPv6 ULA fc00::/7 rejected");
            }
        }
    }

    public static void verifyImageMagicBytes(byte[] data) throws SecurityException {
        if (data.length < 8) {
            throw new SecurityException("File too small to be a valid image");
        }

        // JPEG: FF D8 FF
        boolean isJpeg = (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF);

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        boolean isPng = (data[0] == (byte) 0x89 && data[1] == (byte) 0x50 && data[2] == (byte) 0x4E && data[3] == (byte) 0x47);

        // WebP: RIFF .... WEBP
        boolean isWebP = (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F' &&
                          data.length >= 12 && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P');

        if (!isJpeg && !isPng && !isWebP) {
            throw new SecurityException("Magic bytes validation failed: file is not a supported JPEG, PNG, or WebP image");
        }
    }

    public static String calculateSha256(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 calculation error", e);
        }
    }
}
