package com.vorzak.security;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class HardenedDownloaderTest {

    @Test
    void testRejectsLoopback() throws Exception {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        assertThrows(SecurityException.class, () -> HardenedDownloader.validateIpAddress(loopback));

        InetAddress localhost = InetAddress.getByName("localhost");
        assertThrows(SecurityException.class, () -> HardenedDownloader.validateIpAddress(localhost));
    }

    @Test
    void testRejectsCloudMetadataEndpoint() throws Exception {
        InetAddress metadata = InetAddress.getByName("169.254.169.254");
        assertThrows(SecurityException.class, () -> HardenedDownloader.validateIpAddress(metadata));
    }

    @Test
    void testRejectsPrivateSubnets() throws Exception {
        assertThrows(SecurityException.class, () -> HardenedDownloader.validateIpAddress(InetAddress.getByName("10.0.0.1")));
        assertThrows(SecurityException.class, () -> HardenedDownloader.validateIpAddress(InetAddress.getByName("172.16.0.1")));
        assertThrows(SecurityException.class, () -> HardenedDownloader.validateIpAddress(InetAddress.getByName("192.168.1.1")));
    }

    @Test
    void testRejectsIPv4MappedIPv6Metadata() throws Exception {
        // ::ffff:169.254.169.254
        byte[] mappedMetadata = new byte[] {
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xFF, (byte) 0xFF,
                (byte) 169, (byte) 254, (byte) 169, (byte) 254
        };
        InetAddress ip6Mapped = InetAddress.getByAddress(mappedMetadata);
        assertThrows(SecurityException.class, () -> HardenedDownloader.validateIpAddress(ip6Mapped));
    }

    @Test
    void testMagicBytesValidation() {
        // Valid PNG header
        byte[] validPng = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        assertDoesNotThrow(() -> HardenedDownloader.verifyImageMagicBytes(validPng));

        // Valid JPEG header
        byte[] validJpeg = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46 };
        assertDoesNotThrow(() -> HardenedDownloader.verifyImageMagicBytes(validJpeg));

        // Invalid Text File
        byte[] invalidText = "<html><body>Not an image</body></html>".getBytes();
        assertThrows(SecurityException.class, () -> HardenedDownloader.verifyImageMagicBytes(invalidText));
    }

    @Test
    void testSha256ChecksumIntegrity() {
        byte[] sample = "VORZAK-TEST-PAYLOAD".getBytes();
        String sha = HardenedDownloader.calculateSha256(sample);
        assertNotNull(sha);
        assertEquals(64, sha.length());
    }
}
