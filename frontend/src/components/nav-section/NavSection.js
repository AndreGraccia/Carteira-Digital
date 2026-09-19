import {
  Box,
  List,
  ListItemText,
} from '@mui/material';
import PropTypes from 'prop-types';
import {
  NavLink as RouterLink,
} from 'react-router-dom';
import {
  StyledNavItem,
  StyledNavItemIcon,
} from './styles';

NavSection.propTypes = {
  data: PropTypes.array,
};

export default function NavSection({
  data = [],
  ...other
}) {
  return (
    <Box {...other}>
      <List disablePadding sx={{ py: 1 }}>
        {data.map((item) => (
          <NavItem
            key={item.title}
            item={item}
          />
        ))}
      </List>
    </Box>
  );
}

NavItem.propTypes = {
  item: PropTypes.object,
};

function NavItem({ item }) {
  const {
    title,
    path,
    icon,
    info,
  } = item;

  return (
    <StyledNavItem
      component={RouterLink}
      to={path}
      sx={{
        '&.active': {
          color: 'text.primary',
          bgcolor: 'rgba(255,255,255,0.76)',
          fontWeight: 600,

          boxShadow: `
            0 8px 22px rgba(15,23,42,0.08),
            inset 0 1px 0 rgba(255,255,255,0.86)
          `,

          '&::before': {
            content: '""',
            width: 4,
            height: 22,
            position: 'absolute',
            left: 0,
            borderRadius: '0 6px 6px 0',
            bgcolor: 'primary.main',
            boxShadow:
              '0 0 12px rgba(0,122,255,0.45)',
          },

          '&:hover': {
            bgcolor: 'rgba(255,255,255,0.86)',
          },
        },
      }}
    >
      <StyledNavItemIcon>
        {icon && icon}
      </StyledNavItemIcon>

      <ListItemText
        disableTypography
        primary={title}
      />

      {info && info}
    </StyledNavItem>
  );
}
