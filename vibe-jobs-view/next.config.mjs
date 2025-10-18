
if (!process.env.NEXT_DISABLE_FONT_DOWNLOADS) {
  process.env.NEXT_DISABLE_FONT_DOWNLOADS = '1';
}

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  experimental: { serverActions: { allowedOrigins: ['*'] } },
};
export default nextConfig;
