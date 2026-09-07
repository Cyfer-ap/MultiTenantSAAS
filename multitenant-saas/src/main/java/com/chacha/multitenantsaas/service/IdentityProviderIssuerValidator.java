package com.chacha.multitenantsaas.service;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IdentityProviderIssuerValidator {

    private final HostAddressResolver hostAddressResolver;

    public IdentityProviderIssuerValidator(HostAddressResolver hostAddressResolver) {
        this.hostAddressResolver = hostAddressResolver;
    }

    /**
     * Validates the configured issuer before it is persisted. OIDC metadata retrieval must repeat
     * the public-address check immediately before every server-side request so DNS rebinding cannot
     * bypass this policy.
     */
    public String validateAndNormalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Identity-provider issuer URI must not be blank");
        }

        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Identity-provider issuer URI must be a valid HTTPS URI", exception);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Identity-provider issuer URI must use HTTPS");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("Identity-provider issuer URI must include a host");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "Identity-provider issuer URI must not include user credentials");
        }
        if (uri.getQuery() != null) {
            throw new IllegalArgumentException(
                    "Identity-provider issuer URI must not include a query string");
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Identity-provider issuer URI must not include a fragment");
        }
        if (uri.getPort() == 0 || uri.getPort() < -1 || uri.getPort() > 65535) {
            throw new IllegalArgumentException(
                    "Identity-provider issuer URI contains an invalid port");
        }

        List<InetAddress> addresses;
        try {
            addresses = hostAddressResolver.resolve(uri.getHost());
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException(
                    "Identity-provider issuer host could not be resolved", exception);
        }

        if (addresses == null || addresses.isEmpty()) {
            throw new IllegalArgumentException(
                    "Identity-provider issuer host must resolve to a public address");
        }
        if (addresses.stream().anyMatch(address -> !isPublicDestination(address))) {
            throw new IllegalArgumentException(
                    "Identity-provider issuer host must not resolve to private, local, reserved, or non-routable addresses");
        }

        return uri.normalize().toASCIIString();
    }

    private boolean isPublicDestination(InetAddress address) {
        if (address == null
                || address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        if (address instanceof Inet4Address inet4Address) {
            return isPublicIpv4(inet4Address.getAddress());
        }
        if (address instanceof Inet6Address inet6Address) {
            return isPublicIpv6(inet6Address.getAddress());
        }
        return false;
    }

    private boolean isPublicIpv4(byte[] bytes) {
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        int third = Byte.toUnsignedInt(bytes[2]);

        if (first == 0 || first == 10 || first == 127 || first >= 224) return false;
        if (first == 100 && second >= 64 && second <= 127) return false;
        if (first == 169 && second == 254) return false;
        if (first == 172 && second >= 16 && second <= 31) return false;
        if (first == 192 && second == 168) return false;
        if (first == 192 && second == 0 && (third == 0 || third == 2)) return false;
        if (first == 192 && second == 88 && third == 99) return false;
        if (first == 198 && (second == 18 || second == 19)) return false;
        if (first == 198 && second == 51 && third == 100) return false;
        if (first == 203 && second == 0 && third == 113) return false;
        return true;
    }

    private boolean isPublicIpv6(byte[] bytes) {
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);

        if ((first & 0xFE) == 0xFC) return false;
        return !(first == 0x20
                && second == 0x01
                && Byte.toUnsignedInt(bytes[2]) == 0x0D
                && Byte.toUnsignedInt(bytes[3]) == 0xB8);
    }
}
