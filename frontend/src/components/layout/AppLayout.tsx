import { useState } from 'react';
import type { ReactNode } from 'react';
import {
    AppBar,
    Box,
    Button,
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
import { alpha } from '@mui/material/styles';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined';
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined';
import PeopleAltOutlinedIcon from '@mui/icons-material/PeopleAltOutlined';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import TopicOutlinedIcon from '@mui/icons-material/TopicOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { useAuth } from '../../features/auth/useAuth';
import sakaryaTeknokentLogo from '../../assets/sakarya-teknokent-logo-on-dark.svg';

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
        label: 'Projeler',
        path: '/projects',
        icon: <TopicOutlinedIcon />,
    },
    {
        label: 'TGB Şirketleri',
        path: '/companies/tgb',
        icon: <BusinessOutlinedIcon />,
    },
    {
        label: 'Dış Şirketler',
        path: '/companies/external',
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
    const { role, logout } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const theme = useTheme();
    const isDesktop = useMediaQuery(theme.breakpoints.up('md'));
    const [mobileOpen, setMobileOpen] = useState(false);
    const hasLocalPageHeader = [
        '/academicians',
        '/profile',
        '/users',
        '/projects',
    ].includes(location.pathname)
        || location.pathname.startsWith('/companies/');

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
        <Box
            sx={{
                bgcolor: '#0A1026',
                color: '#F7F9FC',
                display: 'flex',
                flexDirection: 'column',
                height: '100%',
            }}
        >
            <Toolbar
                sx={{
                    justifyContent: 'center',
                    minHeight: { xs: 68, sm: 72 },
                    px: 2.5,
                }}
            >
                <Box
                    component="img"
                    src={sakaryaTeknokentLogo}
                    alt="Sakarya Teknokent"
                    sx={{
                        display: 'block',
                        height: 'auto',
                        maxWidth: '100%',
                        width: 190,
                    }}
                />
            </Toolbar>
            <Divider sx={{ borderColor: 'rgba(255, 255, 255, 0.12)' }} />
            <Box sx={{ flex: 1, minHeight: 0, overflowY: 'auto' }}>
                <List sx={{ px: 1.5, py: 2 }}>
                    {visibleNavigationItems.map((item) => {
                        const selected = location.pathname === item.path;

                        return (
                            <ListItemButton
                                key={item.path}
                                selected={selected}
                                aria-current={selected ? 'page' : undefined}
                                onClick={() => handleNavigate(item.path)}
                                sx={{
                                    borderRadius: 2.5,
                                    color: 'rgba(247, 249, 252, 0.72)',
                                    mb: 0.5,
                                    minHeight: 46,
                                    overflow: 'hidden',
                                    px: 1.5,
                                    position: 'relative',
                                    transition: (theme) => theme.transitions.create(
                                        ['background-color', 'color'],
                                        { duration: theme.transitions.duration.shorter },
                                    ),
                                    '&::before': {
                                        bgcolor: 'transparent',
                                        borderRadius: 999,
                                        bottom: 10,
                                        content: '""',
                                        left: 6,
                                        position: 'absolute',
                                        top: 10,
                                        width: 3,
                                    },
                                    '&:hover': {
                                        bgcolor: 'rgba(255, 255, 255, 0.07)',
                                        color: '#FFFFFF',
                                    },
                                    '&.Mui-selected': {
                                        bgcolor: alpha('#2EA8D3', 0.14),
                                        color: '#2EA8D3',
                                        fontWeight: 700,
                                    },
                                    '&.Mui-selected::before': {
                                        bgcolor: '#2EA8D3',
                                    },
                                    '&.Mui-selected:hover': {
                                        bgcolor: alpha('#2EA8D3', 0.18),
                                    },
                                }}
                            >
                                <ListItemIcon
                                    sx={{
                                        color: 'inherit',
                                        minWidth: 38,
                                        '& .MuiSvgIcon-root': {
                                            fontSize: 22,
                                        },
                                    }}
                                >
                                    {item.icon}
                                </ListItemIcon>
                                <ListItemText
                                    primary={item.label}
                                    slotProps={{
                                        primary: {
                                            sx: {
                                                fontSize: 14,
                                                fontWeight: 'inherit',
                                            },
                                        },
                                    }}
                                />
                            </ListItemButton>
                        );
                    })}
                </List>
            </Box>
            <Box
                sx={{
                    borderTop: '1px solid',
                    borderColor: 'rgba(255, 255, 255, 0.12)',
                    pb: 5,
                    pt: 1.5,
                    px: 1.5,
                }}
            >
                <Stack
                    spacing={1.25}
                    sx={{
                        alignItems: 'stretch',
                        minWidth: 0,
                    }}
                >
                    {role === 'ADMIN' && (
                        <ListItemButton
                            selected={location.pathname === '/profile'}
                            aria-current={
                                location.pathname === '/profile'
                                    ? 'page'
                                    : undefined
                            }
                            onClick={() => handleNavigate('/profile')}
                            sx={{
                                borderRadius: 2.5,
                                color: 'rgba(247, 249, 252, 0.72)',
                                minHeight: 42,
                                px: 1.25,
                                '&:hover': {
                                    bgcolor: 'rgba(255, 255, 255, 0.07)',
                                    color: '#FFFFFF',
                                },
                                '&.Mui-selected': {
                                    bgcolor: alpha('#2EA8D3', 0.14),
                                    color: '#2EA8D3',
                                },
                                '&.Mui-selected:hover': {
                                    bgcolor: alpha('#2EA8D3', 0.18),
                                },
                            }}
                        >
                            <ListItemIcon sx={{ color: 'inherit', minWidth: 38 }}>
                                <PersonOutlineOutlinedIcon />
                            </ListItemIcon>
                            <ListItemText
                                primary="Profil"
                                slotProps={{
                                    primary: {
                                        sx: { fontSize: 14, fontWeight: 600 },
                                    },
                                }}
                            />
                        </ListItemButton>
                    )}
                    <Button
                        color="inherit"
                        startIcon={<LogoutOutlinedIcon />}
                        onClick={handleLogout}
                        sx={{
                            border: '1px solid',
                            borderColor: 'rgba(255, 255, 255, 0.16)',
                            color: 'rgba(247, 249, 252, 0.72)',
                            justifyContent: 'flex-start',
                            minHeight: 36,
                            px: 1.25,
                            width: '100%',
                            '&:hover': {
                                bgcolor: alpha('#2EA8D3', 0.08),
                                borderColor: alpha('#2EA8D3', 0.42),
                                color: '#2EA8D3',
                            },
                        }}
                    >
                        Çıkış
                    </Button>
                </Stack>
            </Box>
        </Box>
    );

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh' }}>
            {!hasLocalPageHeader && (
            <AppBar
                position="absolute"
                color="inherit"
                elevation={0}
                sx={{
                    bgcolor: '#F3F7FA',
                    borderRadius: 0,
                    ml: { md: `${drawerWidth}px` },
                    px: { xs: 1.5, sm: 2 },
                    py: 1.5,
                    width: { md: `calc(100% - ${drawerWidth}px)` },
                }}
            >
                <Toolbar
                    sx={{
                        bgcolor: '#FFFFFF',
                        border: '1px solid #E3EAF0',
                        borderRadius: '12px',
                        boxSizing: 'border-box',
                        gap: { xs: 1.25, sm: 2 },
                        minHeight: { xs: 64, sm: 76 },
                        px: { xs: 2, md: 1, lg: 2 },
                    }}
                >
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

                    <Box
                        sx={{
                            alignItems: 'center',
                            display: 'flex',
                            flexGrow: 1,
                            gap: { xs: 1.25, sm: 1.5 },
                            minWidth: 0,
                        }}
                    >
                        <Box
                            aria-hidden="true"
                            sx={{
                                bgcolor: '#2EA8D3',
                                borderRadius: 999,
                                flex: '0 0 auto',
                                height: { xs: 34, sm: 38 },
                                width: 3,
                            }}
                        />
                        <Stack spacing={0.1} sx={{ minWidth: 0 }}>
                            <Typography
                                variant="h6"
                                noWrap
                                sx={{
                                    fontSize: { xs: 15, sm: 17, md: 20 },
                                    fontWeight: 700,
                                    lineHeight: 1.35,
                                }}
                            >
                                Hakem Atama Yönetim Sistemi
                            </Typography>
                            <Typography
                                color="text.secondary"
                                noWrap
                                sx={{
                                    fontSize: { xs: 11.5, sm: 12.5, md: 14 },
                                    lineHeight: 1.35,
                                }}
                            >
                                Proje değerlendirme ve hakem atama süreçleri
                                için yönetim platformu.
                            </Typography>
                        </Stack>
                    </Box>
                </Toolbar>
            </AppBar>
            )}

            <Box
                component="nav"
                sx={{
                    flexShrink: { md: 0 },
                    width: { md: drawerWidth },
                }}
            >
                <Drawer
                    variant="temporary"
                    open={mobileOpen}
                    onClose={() => setMobileOpen(false)}
                    ModalProps={{ keepMounted: true }}
                    sx={{
                        display: { xs: 'block', md: 'none' },
                        '& .MuiDrawer-paper': {
                            bgcolor: '#0A1026',
                            borderRadius: 0,
                            borderRight: 'none',
                            boxSizing: 'border-box',
                            width: drawerWidth,
                        },
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
                            bgcolor: '#0A1026',
                            borderRadius: 0,
                            borderRight: '1px solid',
                            borderRightColor: 'rgba(255, 255, 255, 0.1)',
                            boxSizing: 'border-box',
                            width: drawerWidth,
                        },
                    }}
                >
                    {drawerContent}
                </Drawer>
            </Box>

            <Box
                component="main"
                sx={{
                    bgcolor: 'background.default',
                    flexGrow: 1,
                    height: hasLocalPageHeader ? '100vh' : undefined,
                    minWidth: 0,
                    overflowX: 'hidden',
                    overflowY: hasLocalPageHeader ? 'auto' : undefined,
                }}
            >
                {!hasLocalPageHeader && (
                    <Toolbar sx={{ minHeight: { xs: 88, sm: 100 } }} />
                )}
                <Box
                    sx={{
                        pb: { xs: 2.5, sm: 3, lg: 4 },
                        px: { xs: 2, sm: 3, lg: 4 },
                        pt: hasLocalPageHeader
                            ? 0
                            : { xs: 2.5, sm: 3, lg: 4 },
                        width: '100%',
                    }}
                >
                    <Outlet />
                </Box>
            </Box>
        </Box>
    );
}
