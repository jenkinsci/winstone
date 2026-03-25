/*
 * Copyright 2003-2026 Olivier Lamy
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.http.ComplianceViolation;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.MultiPartCompliance;
import org.eclipse.jetty.http.UriCompliance;

/**
 * Listener for Jetty HTTP compliance violations.
 * Logs violations at DEBUG level using Jdk logging api.
 *
 */
public class WinstoneViolationListener implements ComplianceViolation.Listener {

    private static final Logger LOGGER = Logger.getLogger(WinstoneViolationListener.class.getName());

    @Override
    public void onComplianceViolation(ComplianceViolation.Event event) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    () -> String.format(
                            "Compliance violation detected: %s - %s (allowed: %s) - details: %s",
                            determineViolationType(event.violation()),
                            event.violation().getName(),
                            event.allowed() ? "allowed" : "forbidden",
                            event.details()));
        }
    }

    private String determineViolationType(ComplianceViolation violation) {
        if (violation instanceof HttpCompliance.Violation) {
            return "HttpCompliance";
        } else if (violation instanceof UriCompliance.Violation) {
            return "UriCompliance";
        } else if (violation instanceof CookieCompliance.Violation) {
            return "CookieCompliance";
        } else if (violation instanceof MultiPartCompliance.Violation) {
            return "MultiPartCompliance";
        } else {
            return "ComplianceViolation";
        }
        //        return (switch (violation){
        //            case HttpCompliance.Violation v -> "HttpCompliance";
        //            case UriCompliance.Violation v -> "UriCompliance";
        //            case CookieCompliance.Violation v -> "CookieCompliance";
        //            case MultiPartCompliance.Violation v -> "MultiPartCompliance";
        //            default -> "ComplianceViolation";
        //        });
    }
}
