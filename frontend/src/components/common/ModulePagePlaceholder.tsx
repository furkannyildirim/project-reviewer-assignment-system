import { Paper, Stack, Typography } from '@mui/material';

type ModulePagePlaceholderProps = {
    title: string;
    description: string;
};

export function ModulePagePlaceholder({
    title,
    description,
}: ModulePagePlaceholderProps) {
    return (
        <Paper
            elevation={0}
            sx={{
                p: { xs: 3, sm: 4 },
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '8px',
            }}
        >
            <Stack spacing={1}>
                <Typography variant="h5" component="h1">
                    {title}
                </Typography>
                <Typography color="text.secondary">
                    {description}
                </Typography>
            </Stack>
        </Paper>
    );
}
