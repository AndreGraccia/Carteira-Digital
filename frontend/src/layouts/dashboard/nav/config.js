import { useTranslation } from 'react-i18next';
import SvgColor from '../../../components/svg-color';

const icon = (name) => (
  <SvgColor
    src={`/assets/icons/navbar/${name}.svg`}
    sx={{ width: 1, height: 1 }}
  />
);

export default function useNavConfig() {
  const { t } = useTranslation();

  return [
    {
      title: t('navigation.dashboard'),
      path: '/',
      icon: icon('ic_analytics'),
    },
    {
      title: t('navigation.wallets'),
      path: '/wallets',
      icon: icon('ic_wallet'),
    },
    {
      title: t('navigation.transfers'),
      path: '/transfers',
      icon: icon('ic_transfer'),
    },
    {
      title: t('navigation.transactions'),
      path: '/transactions',
      icon: icon('ic_transaction'),
    },
  ];
}