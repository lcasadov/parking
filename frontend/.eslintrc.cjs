/* ESLint config — aplica reglas de docs/SONAR-STANDARDS.md (frontend):
   sin var (S3504), sin eval (S4524), complejidad < 15 (S3776),
   a11y en elementos interactivos, key estable (no indice). */
module.exports = {
  root: true,
  env: { browser: true, es2021: true, node: true },
  parser: '@typescript-eslint/parser',
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  settings: { react: { version: 'detect' } },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react/recommended',
    'plugin:react/jsx-runtime',
    'plugin:react-hooks/recommended',
    'plugin:jsx-a11y/recommended',
    'prettier',
  ],
  plugins: ['react-refresh'],
  ignorePatterns: ['dist', 'coverage', 'node_modules', '*.config.ts', '*.config.cjs'],
  rules: {
    'no-var': 'error',
    'prefer-const': 'error',
    eqeqeq: ['error', 'always'],
    'no-eval': 'error',
    'no-implied-eval': 'error',
    complexity: ['error', 15],
    'react/jsx-key': 'error',
    'react/no-array-index-key': 'warn',
    '@typescript-eslint/no-explicit-any': 'error',
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
  },
  overrides: [
    {
      files: ['**/*.test.{ts,tsx}', 'src/test/**', 'src/mocks/**'],
      rules: {
        'react-refresh/only-export-components': 'off',
      },
    },
  ],
};
