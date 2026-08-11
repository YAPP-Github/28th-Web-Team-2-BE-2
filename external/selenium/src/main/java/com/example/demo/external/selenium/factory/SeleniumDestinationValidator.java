package com.example.demo.external.selenium.factory;

import com.example.demo.common.exception.ApiException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.util.Arrays;

final class SeleniumDestinationValidator {

    private SeleniumDestinationValidator() {
    }

    static void validate(final URI targetUrl) {
        if (targetUrl == null || !targetUrl.isAbsolute() || !hasHttpScheme(targetUrl)) {
            throw ApiException.invalidParameter();
        }

        final String host = targetUrl.getHost();
        if (host == null) {
            throw ApiException.invalidParameter();
        }

        try {
            validateResolvedAddresses(host, InetAddress.getAllByName(host));
        } catch (UnknownHostException exception) {
            throw ApiException.invalidParameter();
        }
    }

    static void validateResolvedAddresses(final String host, final InetAddress... addresses) {
        if (addresses.length == 0) {
            throw ApiException.invalidParameter();
        }

        for (final InetAddress address : addresses) {
            if (isBlocked(address)) {
                throw ApiException.invalidParameter();
            }
        }
    }

    private static boolean hasHttpScheme(final URI targetUrl) {
        return "http".equalsIgnoreCase(targetUrl.getScheme())
                || "https".equalsIgnoreCase(targetUrl.getScheme());
    }

    private static boolean isBlocked(final InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalAddress(address)
                || isMappedIpv4AddressBlocked(address);
    }

    private static boolean isUniqueLocalAddress(final InetAddress address) {
        final byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }

    private static boolean isMappedIpv4AddressBlocked(final InetAddress address) {
        final byte[] bytes = address.getAddress();
        if (bytes.length != 16 || !isIpv4MappedAddress(bytes)) {
            return false;
        }

        try {
            return isBlocked(InetAddress.getByAddress(Arrays.copyOfRange(bytes, 12, 16)));
        } catch (UnknownHostException exception) {
            return true;
        }
    }

    private static boolean isIpv4MappedAddress(final byte[] bytes) {
        for (int index = 0; index < 10; index++) {
            if (bytes[index] != 0) {
                return false;
            }
        }
        return bytes[10] == -1 && bytes[11] == -1;
    }
}
