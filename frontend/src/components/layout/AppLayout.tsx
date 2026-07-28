import { useState } from 'react';
import type { ReactNode } from 'react';
import {
    AppBar,
    Avatar,
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
    const { role, logout } = useAuth();
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
        <Box
            sx={{
                bgcolor: 'background.paper',
                display: 'flex',
                flexDirection: 'column',
                height: '100%',
            }}
        >
            <Toolbar
                sx={{
                    minHeight: { xs: 68, sm: 72 },
                    px: 2.5,
                }}
            >
                <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', minWidth: 0 }}>
                    <Box
                        aria-hidden="true"
                        sx={{
                            alignItems: 'center',
                            bgcolor: 'primary.light',
                            border: '1px solid',
                            borderColor: (theme) => alpha(theme.palette.primary.main, 0.16),
                            borderRadius: 2.5,
                            color: 'primary.main',
                            display: 'flex',
                            flexShrink: 0,
                            fontSize: 18,
                            fontWeight: 800,
                            height: 40,
                            justifyContent: 'center',
                            width: 40,
                        }}
                    >
                        H
                    </Box>
                    <Box sx={{ minWidth: 0 }}>
                        <Typography variant="h6" noWrap sx={{ fontSize: 18 }}>
                            Hakem Atama
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            Yönetim paneli
                        </Typography>
                    </Box>
                </Stack>
            </Toolbar>
            <Divider />
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
                                    color: 'text.secondary',
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
                                        bgcolor: (theme) => alpha(theme.palette.primary.main, 0.05),
                                        color: 'text.primary',
                                    },
                                    '&.Mui-selected': {
                                        bgcolor: 'primary.light',
                                        color: 'primary.main',
                                        fontWeight: 700,
                                    },
                                    '&.Mui-selected::before': {
                                        bgcolor: 'primary.main',
                                    },
                                    '&.Mui-selected:hover': {
                                        bgcolor: 'primary.light',
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
                    borderColor: 'divider',
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
                    <Stack
                        direction="row"
                        spacing={1}
                        sx={{
                            alignItems: 'center',
                            minWidth: 0,
                            px: 0.5,
                        }}
                    >
                        <Avatar
                            sx={{
                                bgcolor: 'primary.light',
                                color: 'primary.main',
                                fontSize: 13,
                                fontWeight: 800,
                                height: 34,
                                width: 34,
                            }}
                        >
                            P
                        </Avatar>
                        <Typography
                            variant="body2"
                            color="text.primary"
                            noWrap
                            sx={{ fontWeight: 600 }}
                        >
                            Profil
                        </Typography>
                    </Stack>
                    <Button
                        color="inherit"
                        startIcon={<LogoutOutlinedIcon />}
                        onClick={handleLogout}
                        sx={{
                            border: '1px solid',
                            borderColor: 'divider',
                            color: 'text.secondary',
                            justifyContent: 'flex-start',
                            minHeight: 36,
                            px: 1.25,
                            width: '100%',
                            '&:hover': {
                                borderColor: (theme) => alpha(theme.palette.primary.main, 0.28),
                                color: 'primary.main',
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
            <AppBar
                position="fixed"
                color="inherit"
                elevation={0}
                sx={{
                    bgcolor: 'background.paper',
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                    ml: { md: `${drawerWidth}px` },
                    width: { md: `calc(100% - ${drawerWidth}px)` },
                }}
            >
                <Toolbar
                    sx={{
                        gap: { xs: 1.25, sm: 2 },
                        minHeight: { xs: 64, sm: 68 },
                        px: { xs: 2, sm: 3 },
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

                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                        <Typography
                            variant="h6"
                            noWrap
                            sx={{
                                fontSize: { xs: 16, sm: 18 },
                                fontWeight: 700,
                            }}
                        >
                            Hakem Atama Yönetim Sistemi
                        </Typography>
                    </Box>
                </Toolbar>
            </AppBar>

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
                            borderRight: '1px solid',
                            borderRightColor: 'divider',
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
                    minWidth: 0,
                    overflowX: 'hidden',
                }}
            >
                <Toolbar sx={{ minHeight: { xs: 64, sm: 68 } }} />
                <Box
                    sx={{
                        px: { xs: 2, sm: 3, lg: 4 },
                        py: { xs: 2.5, sm: 3, lg: 4 },
                        width: '100%',
                    }}
                >
                    <Outlet />
                </Box>
            </Box>
        </Box>
    );
}
