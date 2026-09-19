import {
  ListItemButton,
  ListItemIcon,
} from '@mui/material';
import { styled } from '@mui/material/styles';

export const StyledNavItem = styled(
  (props) => (
    <ListItemButton
      disableGutters
      {...props}
    />
  )
)(({ theme }) => ({
  ...theme.typography.body2,

  height: 50,
  margin: theme.spacing(0.5, 1.5),
  padding: theme.spacing(0, 1.75),
  position: 'relative',

  color: theme.palette.text.secondary,
  borderRadius: 16,
  fontSize: 15,
  fontWeight: 500,

  transition:
    'color 180ms ease, background 180ms ease, box-shadow 180ms ease',

  '&:hover': {
    color: theme.palette.text.primary,
    backgroundColor:
      'rgba(255,255,255,0.54)',
  },
}));

export const StyledNavItemIcon =
  styled(ListItemIcon)({
    width: 22,
    height: 22,
    minWidth: 40,
    color: 'inherit',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'flex-start',
  });
  