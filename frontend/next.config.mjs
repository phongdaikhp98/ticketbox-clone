/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",

  // [SECURITY] Add baseline HTTP security headers to every response (H1).
  // These are defence-in-depth headers that browsers enforce regardless of app code.
  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [
          // Prevent browsers from MIME-sniffing a response away from the declared type
          { key: "X-Content-Type-Options", value: "nosniff" },
          // Disallow embedding this app inside iframes (clickjacking defence)
          { key: "X-Frame-Options", value: "DENY" },
          // Enable legacy XSS filter in older browsers
          { key: "X-XSS-Protection", value: "1; mode=block" },
          // Only send full URL referrer to same origin; strip to origin for cross-origin
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          // Opt out of all browser APIs the app does not use
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
        ],
      },
    ];
  },
};

export default nextConfig;
