"""Builds one JSON response per endpoint from the official Open Finance Brasil OpenAPI files,
using the `example` each field carries in the spec. Output: backend test fixtures."""
import json, os, sys, yaml

SRC = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[1]

def load(name):
    return yaml.safe_load(open(os.path.join(SRC, name), encoding='utf-8'))

def resolve(spec, node):
    while isinstance(node, dict) and '$ref' in node:
        parts = node['$ref'].lstrip('#/').split('/')
        target = spec
        for p in parts:
            target = target[p]
        node = target
    return node

def sample(spec, node, depth=0):
    node = resolve(spec, node)
    if depth > 12 or not isinstance(node, dict):
        return None
    if 'allOf' in node:
        merged = {}
        for part in node['allOf']:
            value = sample(spec, part, depth + 1)
            if isinstance(value, dict):
                merged.update(value)
        return merged
    for key in ('oneOf', 'anyOf'):
        if key in node:
            return sample(spec, node[key][0], depth + 1)
    if 'example' in node and node.get('type') != 'object' and node.get('type') != 'array':
        return node['example']
    kind = node.get('type')
    if kind == 'object' or 'properties' in node:
        return {name: sample(spec, prop, depth + 1) for name, prop in node.get('properties', {}).items()}
    if kind == 'array':
        item = sample(spec, node.get('items', {}), depth + 1)
        return [item] if item is not None else []
    if 'enum' in node:
        return node['enum'][0]
    if kind == 'string':
        return {'date': '2026-01-15', 'date-time': '2026-01-15T10:00:00Z'}.get(node.get('format'), 'x')
    if kind in ('number', 'integer'):
        return 1
    if kind == 'boolean':
        return False
    return None

def response(spec, name):
    r = resolve(spec, spec['components']['responses'][name])
    return sample(spec, r['content']['application/json']['schema'])

TARGETS = {
    'accounts.yml': ['OKResponseAccountList', 'OKResponseAccountBalances', 'OKResponseAccountTransactions'],
    'cards.yml': ['OKResponseCreditCardAccountsList', 'OKResponseCreditCardAccountsBills',
                  'OKResponseCreditCardAccountsLimits', 'OKResponseCreditCardAccountsTransactions'],
    'loans.yml': ['OKResponseLoansContractList', 'OKResponseLoansContract', 'OKResponseLoansInstalments', 'OKResponseLoansPayments'],
    'resources.yml': ['OKResponseResourceList'],

}

os.makedirs(OUT, exist_ok=True)
for file, names in TARGETS.items():
    spec = load(file)
    version = spec['info']['version']
    for name in names:
        if name not in spec['components']['responses']:
            print('skip', file, name)
            continue
        out = os.path.join(OUT, name + '.json')
        json.dump(response(spec, name), open(out, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
        print(file, version, '->', os.path.basename(out))
