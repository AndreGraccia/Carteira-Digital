const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
process.chdir(path.resolve(__dirname, '../..'));
const i18next = require(path.resolve('frontend/node_modules/i18next/dist/cjs/i18next.js'));
const i18n = i18next.createInstance();
const source = fs
  .readFileSync('frontend/src/i18n.js', 'utf8')
  .replace(/^import .*;\r?\n/gm, '')
  .replace('export default i18n;', '');
vm.runInNewContext(source, {
  i18n,
  initReactI18next: { type: '3rdParty', init() {} },
  localStorage: {
    getItem() {
      return 'pt';
    },
    setItem() {},
  },
  navigator: { language: 'pt-BR' },
  document: { documentElement: {} },
});
assert.equal(i18n.t('wallet.card.clickToOpen'), 'Ver extrato');
assert.equal(i18n.t('insights.opportunity.years', { count: 1 }), '1 ano');
assert.equal(i18n.t('insights.opportunity.years', { count: 2 }), '2 anos');
i18n.changeLanguage('en');
assert.equal(i18n.t('wallet.card.clickToOpen'), 'View statement');
assert.equal(i18n.t('insights.opportunity.years', { count: 1 }), '1 year');
assert.equal(i18n.t('insights.opportunity.years', { count: 2 }), '2 years');
assert.equal(i18n.t('transactions.types.NewType', { defaultValue: 'NewType' }), 'NewType');
// The backend records which consent text the person read (B42). Changing that text without a new version would make
// the record lie, so the text is fingerprinted: a change fails here until consentTerms.json gets a new version and hash,
// and the backend (OpenFinanceConsentRecord.TERMS_VERSIONS) accepts that version.
const consentTerms = JSON.parse(fs.readFileSync('frontend/src/consentTerms.json', 'utf8'));
const consentText = ['pt', 'en'].map((language) => {
  const { consent, permissions, permissionGroups } = i18n.getResourceBundle(language, 'translation').openFinance;
  return { consent, permissions, permissionGroups };
});
const consentHash = require('node:crypto').createHash('sha256').update(JSON.stringify(consentText)).digest('hex');
assert.equal(
  consentHash,
  consentTerms.sha256,
  `The Open Finance consent text changed: give frontend/src/consentTerms.json a new version and sha256 ${consentHash}.`
);
console.log('PASS: real i18next language switching, card labels, plurals and unknown-type fallback.');
