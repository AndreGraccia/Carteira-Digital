from pathlib import Path
p=Path('README.md');s=p.read_text(encoding='utf-8');a=s.index('## Interface e regras');b=s.index('## Funcionalidades atuais',a);s=s[:a]+'## Interface e regras da demonstra\u00e7\u00e3o\n\nA experi\u00eancia de carteiras, movimenta\u00e7\u00f5es, rotas e tradu\u00e7\u00f5es foi revisada. Consulte [design, regras implementadas e limites do prot\u00f3tipo](docs/wallet-design-and-rules.md), incluindo a migra\u00e7\u00e3o V6 e as refer\u00eancias oficiais da Apple e do Banco Central.\n\n'+s[b:];p.write_text(s,encoding='utf-8')
p=Path('frontend/src/pages/transfer/BasicTabs.js');s=p.read_text();s=s.replace("  const initialWallet = params.get('wallet') || '';\n  const [selectedId, setSelectedId] = useState(initialWallet);", "  const selectedId = params.get('wallet') || '';")
s=s.replace('  const requestId = useRef(null);','  const requestId = useRef(null);\n  const requestSignature = useRef(null);')
s=s.replace('      if (!requestId.current) requestId.current = crypto.randomUUID();','''      const signature = JSON.stringify([mode, wallet.iban, destination, cents, description.trim()]);
      if (!requestId.current || requestSignature.current !== signature) {
        requestId.current = crypto.randomUUID();
        requestSignature.current = signature;
      }''')
s=s.replace('setSelectedId(event.target.value);','setParams({ mode, wallet: event.target.value });');p.write_text(s,encoding='utf-8')
p=Path('frontend/src/pages/transaction/Transaction.js');s=p.read_text();s=s.replace("  const selected = params.get('wallet') || '';", "  const requestedWallet = params.get('wallet') || '';\n  const selected = wallets.some((wallet) => String(wallet.id) === requestedWallet) ? requestedWallet : '';");p.write_text(s,encoding='utf-8')
