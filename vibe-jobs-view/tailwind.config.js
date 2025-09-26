
const uiExtend = require('./vibe-jobs-ui-pack/tailwind.extend.js');

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./app/**/*.{js,ts,jsx,tsx}", "./components/**/*.{js,ts,jsx,tsx}", "./vibe-jobs-ui-pack/**/*.{js,ts,jsx,tsx}", "./lib/**/*.{js,ts,jsx,tsx}", "./styles/**/*.{css,scss}"],
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
