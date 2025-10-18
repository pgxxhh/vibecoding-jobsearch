
const uiExtend = require('./vibe-jobs-ui-pack/tailwind.extend.js');

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
    './src/modules/**/*.{js,ts,jsx,tsx,mdx}',
    './src/shared/**/*.{js,ts,jsx,tsx,mdx}',
    './vibe-jobs-ui-pack/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      ...uiExtend,
      maxWidth: {
        '8xl': '88rem',
      },
    },
  },
  plugins: [],
};
