from pathlib import Path
p=Path('frontend/src/pages/auth/AuthView.js');s=p.read_text().replace('  Grid,\n','');p.write_text(s,encoding='utf-8')
p=Path('frontend/src/pages/wallet/Wallet.js');s=p.read_text().replace('{ Box, Button,','{ Button,');p.write_text(s,encoding='utf-8')
p=Path('frontend/src/pages/transfer/BasicTabs.js');s=p.read_text();s=s.replace('    if (!requestId.current) requestId.current = crypto.randomUUID();\n    try {','    try {\n      if (!requestId.current) requestId.current = crypto.randomUUID();');p.write_text(s,encoding='utf-8')
p=Path('backend/src/main/java/com/github/yildizmy/service/AuthService.java');s=p.read_text().replace('    public CommandResponse signup(SignupRequest request) {','    public CommandResponse signup(SignupRequest request) {\n        request.setId(null);\n        request.setRoles(java.util.Set.of("ROLE_USER"));');p.write_text(s,encoding='utf-8')
p=Path('backend/src/main/java/com/github/yildizmy/dto/mapper/SignupRequestMapper.java');s=p.read_text().replace('    @Mapping(target = "roles", ignore = true)','    @Mapping(target = "roles", ignore = true)\n    @Mapping(target = "id", ignore = true)');p.write_text(s,encoding='utf-8')
p=Path('backend/src/main/java/com/github/yildizmy/dto/mapper/TransactionResponseMapper.java');s=p.read_text();s=s.replace('    @Mapping(target = "createdAt", ignore = true)','''    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "fromWallet.balance", ignore = true)
    @Mapping(target = "fromWallet.user", ignore = true)
    @Mapping(target = "toWallet.balance", ignore = true)
    @Mapping(target = "toWallet.user", ignore = true)''');p.write_text(s,encoding='utf-8')
p=Path('frontend/scripts/check-wallet.cjs');p.parent.mkdir(exist_ok=True);s=Path('.wallet-check.cjs').read_text(encoding='utf-8-sig');s=s.replace("const path = require('node:path');", "const path = require('node:path');\nprocess.chdir(path.resolve(__dirname, '../..'));");p.write_text(s,encoding='utf-8')
import json
p=Path('frontend/package.json');d=json.loads(p.read_text());d['scripts']['test:wallet']='node scripts/check-wallet.cjs';p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
