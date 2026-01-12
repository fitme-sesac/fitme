import { useEffect } from "react";

/**
 * Renders server-rendered HTML fragments (from the legacy Thymeleaf templates)
 * while we transition to full React components.
 *
 * - html: string containing markup (no inline <script> execution)
 * - scripts: array of inline script bodies (executed by injecting <script> tags)
 */
export default function HtmlPage({ html, scripts = [] }) {
  // Run page-level scripts (legacy behavior)
  useEffect(() => {
    if (!scripts?.length) return;

    const tags = scripts.map((code) => {
      const s = document.createElement("script");
      s.type = "text/javascript";
      s.text = code;
      document.body.appendChild(s);
      return s;
    });

    return () => {
      tags.forEach((t) => t.remove());
    };
  }, [scripts]);

  // Hide empty alert boxes and render query-param messages into them.
  // This fixes the "empty red box" issue across legacy HTML pages.
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);

    const errorMessage = (params.get("errorMessage") || params.get("error") || "").trim();
    const successMessage = (params.get("message") || params.get("successMessage") || "").trim();

    const setAlertText = (selector, msg) => {
      const nodes = Array.from(document.querySelectorAll(selector));
      nodes.forEach((el, idx) => {
        if (msg) {
          // Only show the first matching box to avoid duplicates
          if (idx === 0) {
            const span = el.querySelector("span");
            if (span) span.textContent = msg;
            else el.textContent = msg;
            el.style.display = "";
          } else {
            el.style.display = "none";
          }
        } else {
          // No message: hide if empty/whitespace, keep if it already has content
          if (!el.textContent || !el.textContent.trim()) {
            el.style.display = "none";
          }
        }
      });
    };

    setAlertText(".alert.alert-danger", errorMessage);
    setAlertText(".alert.alert-success", successMessage);

    // Final pass: hide any remaining empty alerts
    document.querySelectorAll(".alert").forEach((el) => {
      if (!el.textContent || !el.textContent.trim()) {
        el.style.display = "none";
      }
    });
  }, [html]);

  // eslint-disable-next-line react/no-danger
  return <div dangerouslySetInnerHTML={{ __html: html }} />;
}
