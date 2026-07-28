import { alpha, createTheme } from '@mui/material/styles';

export const theme = createTheme({
    palette: {
        mode: 'light',
        primary: {
            main: '#5B3FD6',
            dark: '#4930BC',
            light: '#F0ECFF',
            contrastText: '#FFFFFF',
        },
        secondary: {
            main: '#0891b2',
        },
        divider: '#E5E7EB',
        background: {
            default: '#F6F7FB',
            paper: '#FFFFFF',
        },
        text: {
            primary: '#172033',
            secondary: '#667085',
        },
    },
    typography: {
        fontFamily: '"Segoe UI", Roboto, Arial, sans-serif',
        fontSize: 14,
        h4: {
            fontWeight: 700,
            letterSpacing: 0,
        },
        h6: {
            fontWeight: 600,
            letterSpacing: 0,
        },
        button: {
            fontWeight: 600,
            textTransform: 'none',
            letterSpacing: 0,
        },
    },
    shape: {
        borderRadius: 12,
    },
    components: {
        MuiButton: {
            defaultProps: {
                disableElevation: true,
            },
            styleOverrides: {
                root: ({ theme }) => ({
                    borderRadius: 10,
                    boxShadow: 'none',
                    minHeight: 40,
                    paddingLeft: theme.spacing(2),
                    paddingRight: theme.spacing(2),
                    '&:focus-visible': {
                        outline: `3px solid ${alpha(theme.palette.primary.main, 0.28)}`,
                        outlineOffset: 2,
                    },
                    '&.MuiButton-containedPrimary': {
                        backgroundColor: theme.palette.primary.main,
                        '&:hover': {
                            backgroundColor: theme.palette.primary.dark,
                            boxShadow: 'none',
                        },
                    },
                    '&.MuiButton-outlined': {
                        borderColor: theme.palette.divider,
                        '&:hover': {
                            backgroundColor: alpha(theme.palette.primary.main, 0.04),
                            borderColor: alpha(theme.palette.primary.main, 0.45),
                        },
                    },
                }),
            },
        },
        MuiPaper: {
            styleOverrides: {
                root: ({ theme }) => ({
                    backgroundImage: 'none',
                    borderRadius: theme.shape.borderRadius,
                }),
            },
        },
        MuiTextField: {
            defaultProps: {
                size: 'small',
            },
        },
        MuiSelect: {
            defaultProps: {
                size: 'small',
            },
        },
        MuiOutlinedInput: {
            styleOverrides: {
                root: ({ theme }) => ({
                    backgroundColor: theme.palette.background.paper,
                    borderRadius: 10,
                    '& .MuiOutlinedInput-notchedOutline': {
                        borderColor: theme.palette.divider,
                    },
                    '&:hover .MuiOutlinedInput-notchedOutline': {
                        borderColor: alpha(theme.palette.text.primary, 0.28),
                    },
                    '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                        borderColor: theme.palette.primary.main,
                        borderWidth: 1,
                        boxShadow: `0 0 0 3px ${alpha(theme.palette.primary.main, 0.14)}`,
                    },
                }),
                input: {
                    paddingBottom: 9,
                    paddingTop: 9,
                },
            },
        },
        MuiChip: {
            styleOverrides: {
                root: ({ theme }) => ({
                    borderRadius: 999,
                    fontWeight: 600,
                    '&.MuiChip-colorDefault': {
                        color: theme.palette.text.primary,
                    },
                    '&.MuiChip-colorPrimary': {
                        backgroundColor: theme.palette.primary.light,
                        color: theme.palette.primary.main,
                    },
                    '&.MuiChip-outlinedPrimary': {
                        backgroundColor: theme.palette.primary.light,
                        borderColor: alpha(theme.palette.primary.main, 0.24),
                        color: theme.palette.primary.main,
                    },
                }),
            },
        },
        MuiTooltip: {
            styleOverrides: {
                tooltip: ({ theme }) => ({
                    backgroundColor: theme.palette.text.primary,
                    borderRadius: 8,
                    fontSize: 12,
                    lineHeight: 1.4,
                    padding: '6px 9px',
                }),
            },
        },
        MuiIconButton: {
            styleOverrides: {
                root: ({ theme }) => ({
                    borderRadius: 10,
                    color: theme.palette.text.secondary,
                    '&:hover': {
                        backgroundColor: alpha(theme.palette.primary.main, 0.07),
                        color: theme.palette.primary.main,
                    },
                    '&:focus-visible': {
                        outline: `3px solid ${alpha(theme.palette.primary.main, 0.26)}`,
                        outlineOffset: 2,
                    },
                }),
            },
        },
        MuiCssBaseline: {
            styleOverrides: {
                body: {
                    backgroundColor: '#F6F7FB',
                },
            },
        },
    },
});
