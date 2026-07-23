import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Stack,
    Typography,
} from '@mui/material';

type ConfirmDialogProps = {
    open: boolean;
    title: string;
    description: string;
    confirmLabel: string;
    isConfirming: boolean;
    errorMessage?: string | null;
    onClose: () => void;
    onConfirm: () => void;
};

export function ConfirmDialog({
    open,
    title,
    description,
    confirmLabel,
    isConfirming,
    errorMessage,
    onClose,
    onConfirm,
}: ConfirmDialogProps) {
    return (
        <Dialog open={open} onClose={isConfirming ? undefined : onClose} fullWidth maxWidth="xs">
            <DialogTitle>{title}</DialogTitle>
            <DialogContent>
                <Stack spacing={2}>
                    {errorMessage && (
                        <Alert severity="error">{errorMessage}</Alert>
                    )}
                    <Typography color="text.secondary">
                        {description}
                    </Typography>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={isConfirming}>
                    Vazgeç
                </Button>
                <Button
                    color="error"
                    variant="contained"
                    loading={isConfirming}
                    onClick={onConfirm}
                >
                    {confirmLabel}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
