import { useState } from 'react';
import type { ReactNode } from 'react';
import {
    AppBar,
    Box,
    Button,
    Chip,
    Divider,
    Drawer,
    IconButton,
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Stack,
    Toolbar,
    Typography,
    useMediaQuery,
    useTheme,
} from '@mui/material';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined';
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined';
import PeopleAltOutlinedIcon from '@mui/icons-material/PeopleAltOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { useAuth } from '../../features/auth/useAuth';

const drawerWidth = 260;

type NavigationItem = {
    label: string;
    path: string;
    icon: ReactNode;
    adminOnly?: boolean;
};

const navigationItems: NavigationItem[] = [
    {
        label: 'Akademisyenler',
        path: '/academicians',
        icon: <SchoolOutlinedIcon />,
    },
    {
        label: 'Şirketler',
        path: '/companies',
        icon: <BusinessOutlinedIcon />,
    },
    {
        label: 'Kullanıcılar',
        path: '/users',
        icon: <PeopleAltOutlinedIcon />,
        adminOnly: true,
    },
];

export function AppLayout() {
    const { currentUser, role, logout } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const theme = useTheme();
    const isDesktop = useMediaQuery(theme.breakpoints.up('md'));
    const [mobileOpen, setMobileOpen] = useState(false);

    const visibleNavigationItems = navigationItems.filter(
        (item) => !item.adminOnly || role === 'ADMIN',
    );

    function handleNavigate(path: string) {
        navigate(path);
        setMobileOpen(false);
    }

    function handleLogout() {
        logout();
        navigate('/login', { replace: true });
    }

    const drawerContent = (
        <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Toolbar>
                <Stack spacing={0.25}>
                    <Typography variant="h6" noWrap>
                        Hakem Atama
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                        Yönetim paneli
                    </Typography>
                </Stack>
            </Toolbar>
            <Divider />
            <List sx={{ px: 1.5, py: 2 }}>
                {visibleNavigationItems.map((item) => (
                    <ListItemButton
                        key={item.path}
                        selected={location.pathname === item.path}
                        onClick={() => handleNavigate(item.path)}
                        sx={{ mb: 0.5, borderRadius: '8px' }}
                    >
                        <ListItemIcon sx={{ minWidth: 40 }}>
                            {item.icon}
                        </ListItemIcon>
                        <ListItemText primary={item.label} />
                    </ListItemButton>
                ))}
            </List>
        </Box>
    );

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh' }}>
            <AppBar
                position="fixed"
                color="inherit"
                elevation={0}
                sx={{
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                    width: { md: `calc(100% - ${drawerWidth}px)` },
                    ml: { md: `${drawerWidth}px` },
                }}
            >
                <Toolbar sx={{ gap: 2 }}>
                    {!isDesktop && (
                        <IconButton
                            color="inherit"
                            edge="start"
                            aria-label="Menüyü aç"
                            onClick={() => setMobileOpen(true)}
                        >
                            <MenuOutlinedIcon />
                        </IconButton>
                    )}

                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                        <Typography variant="h6" noWrap>
                            Hakem Atama Yönetim Sistemi
                        </Typography>
                    </Box>

                    <Stack
                        direction="row"
                        spacing={1}
                        sx={{
                            display: { xs: 'none', sm: 'flex' },
                            alignItems: 'center',
                            minWidth: 0,
                        }}
                    >
                        <Typography
                            variant="body2"
                            color="text.secondary"
                            noWrap
                            sx={{ maxWidth: 240 }}
                        >
                            {currentUser?.email}
                        </Typography>
                        {role && (
                            <Chip
                                label={role}
                                color={role === 'ADMIN' ? 'primary' : 'default'}
                                size="small"
                                variant="outlined"
                            />
                        )}
                    </Stack>

                    <Button
                        color="inherit"
                        startIcon={<LogoutOutlinedIcon />}
                        onClick={handleLogout}
                    >
                        Çıkış yap
                    </Button>
                </Toolbar>
            </AppBar>

            <Box
                component="nav"
                sx={{
                    width: { md: drawerWidth },
                    flexShrink: { md: 0 },
                }}
            >
                <Drawer
                    variant="temporary"
                    open={mobileOpen}
                    onClose={() => setMobileOpen(false)}
                    ModalProps={{ keepMounted: true }}
                    sx={{
                        display: { xs: 'block', md: 'none' },
                        '& .MuiDrawer-paper': { width: drawerWidth },
                    }}
                >
                    {drawerContent}
                </Drawer>

                <Drawer
                    variant="permanent"
                    open
                    sx={{
                        display: { xs: 'none', md: 'block' },
                        '& .MuiDrawer-paper': {
                            width: drawerWidth,
                            boxSizing: 'border-box',
                        },
                    }}
                >
                    {drawerContent}
                </Drawer>
            </Box>

            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    minWidth: 0,
                    bgcolor: 'background.default',
                    p: { xs: 2, sm: 3 },
                }}
            >
                <Toolbar />
                <Outlet />
            </Box>
        </Box>
    );
}
