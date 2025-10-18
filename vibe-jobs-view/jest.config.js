const nextJest = require('next/jest');

const createJestConfig = nextJest({
  dir: './',
});

const customJestConfig = {
  testEnvironment: 'node',
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '^@/modules/(.*)$': '<rootDir>/src/modules/$1',
    '^@/shared/(.*)$': '<rootDir>/src/shared/$1',
    '^@/vibe-jobs-ui-pack/(.*)$': '<rootDir>/vibe-jobs-ui-pack/$1',
  },
  setupFilesAfterEnv: [],
};

module.exports = createJestConfig(customJestConfig);
