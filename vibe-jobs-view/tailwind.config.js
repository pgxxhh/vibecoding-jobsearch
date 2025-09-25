
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./app/**/*.{js,ts,jsx,tsx}", "./components/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#fff5f8',
          100: '#ffe8f0',
          200: '#ffcfe0',
          300: '#ffa6c5',
          400: '#ff7baa',
          500: '#f54d92',
          600: '#d7367c',
          700: '#b02565',
          800: '#85184d',
          900: '#5b0d36',
        },
        accent: {
          50: '#fdf2ff',
          100: '#f8d8fe',
          200: '#f0b2fd',
          300: '#e284f9',
          400: '#d258f3',
          500: '#b136d9',
          600: '#8d27ae',
          700: '#6a1c86',
          800: '#4b1360',
          900: '#300a3d',
        },
      },
      boxShadow: {
        'glow': '0 10px 30px -10px rgba(245, 77, 146, 0.4)',
      },
      backgroundImage: {
        'brand-gradient': 'linear-gradient(135deg, #ffe8f0 0%, #f54d92 50%, #8d27ae 100%)',
      },
    },
  },
  plugins: [],
};
