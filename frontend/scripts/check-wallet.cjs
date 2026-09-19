const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
process.chdir(path.resolve(__dirname, '../..'));
const wallet = fs.readFileSync('frontend/src/utils/wallet.js', 'utf8').replace(/export /g, '');
const api = vm.runInNewContext(wallet + '; ({parseAmount, directionFor, totalsFor, normalizeTransactions, money, errorKey})');
assert.equal(api.parseAmount('25,50'), 2550);
assert.equal(api.parseAmount('0.01'), 1);
assert.equal(api.parseAmount('9999999999.99'), 999999999999);
for (const invalid of ['-1', '1.001', 'NaN', '1e3', '1,000.00', '', 'Infinity', '10000000000']) assert.equal(api.parseAmount(invalid), null, invalid);
const wallets = [{id: 1, iban: 'A'}, {id: 2, iban: 'B'}];
const transfer = {id: 1, fromWallet: wallets[0], toWallet: wallets[1], amount: '15.25', status: 'SUCCESS', type: {name: 'Transfer'}, createdAt: '2026-09-18T10:00:00Z'};
assert.equal(api.directionFor(transfer, wallets), 'internal');
assert.equal(api.directionFor(transfer, [wallets[0]]), 'outgoing');
assert.equal(api.directionFor(transfer, [wallets[1]]), 'incoming');
const deposit = {...transfer, id:2, toWallet:wallets[0], type:{name:'Deposit'}};
const withdrawal = {...deposit, id:3, type:{name:'Withdrawal'}};
assert.equal(api.directionFor(deposit, wallets), 'incoming');
assert.equal(api.directionFor(withdrawal, wallets), 'outgoing');
const total = api.totalsFor([transfer, deposit, withdrawal, {...deposit, status:'ERROR'}], wallets);
assert.equal(total.incoming,1525); assert.equal(total.outgoing,1525);
assert.equal(api.normalizeTransactions({content:[transfer,transfer,deposit]}).length,2);
assert.equal(api.errorKey({response:{status:412}}),'app.errors.balance');
let resources;
const i18n = {on(){},use(){return this;},init(options){resources=options.resources;}};
vm.runInNewContext(fs.readFileSync('frontend/src/i18n.js','utf8').replace(/^import .*;\r?\n/gm,'').replace('export default i18n;',''), {i18n,initReactI18next:{},localStorage:{getItem(){return 'pt';},setItem(){}},navigator:{language:'pt-BR'},document:{documentElement:{}}});
function flatten(obj, prefix='', result={}) {for(const [key,value] of Object.entries(obj)){const full=prefix?`${prefix}.${key}`:key;if(typeof value==='string')result[full]=value;else flatten(value,full,result);}return result;}
const pt=flatten(resources.pt.translation);const en=flatten(resources.en.translation);
assert.deepEqual(Object.keys(pt).sort(),Object.keys(en).sort());
function files(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(entry=>entry.isDirectory()?files(path.join(dir,entry.name)):[path.join(dir,entry.name)]);}
let count=0;
for(const file of files('frontend/src').filter(f=>f.endsWith('.js')&&!f.endsWith('i18n.js'))){for(const match of fs.readFileSync(file,'utf8').matchAll(/\bt\(\s*['"]([^'"]+)['"]/g)){assert.ok(match[1] in pt || `${match[1]}_one` in pt, `${file}: missing ${match[1]}`);count++;}}
for (const key of Object.keys(pt)) {assert.ok(pt[key].trim()&&en[key].trim());assert.deepEqual(pt[key].match(/{{.*?}}/g),en[key].match(/{{.*?}}/g),key);}
console.log(`PASS: amount parsing, cent precision, internal transfers, deposits, withdrawals, failed transactions, duplicate rows, error messages; ${Object.keys(pt).length} PT/EN keys; ${count} literal translation references.`);
