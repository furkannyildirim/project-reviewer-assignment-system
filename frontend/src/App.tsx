import {
  Box,
  Button,
  Container,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';

function App() {
  return (
      <Box
          sx={{
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            py: 4,
          }}
      >
        <Container maxWidth="md">
          <Paper
              elevation={0}
              sx={{
                p: { xs: 3, sm: 5 },
                border: '1px solid',
                borderColor: 'divider',
              }}
          >
            <Stack spacing={3} sx={{ alignItems: 'flex-start' }}>
              <Box
                  sx={{
                    width: 52,
                    height: 52,
                    display: 'grid',
                    placeItems: 'center',
                    borderRadius: 2,
                    color: 'primary.main',
                    bgcolor: 'primary.50',
                  }}
              >
                <DashboardOutlinedIcon fontSize="large" />
              </Box>

              <Stack spacing={1}>
                <Typography variant="h4">
                  Hakem Atama Yönetim Sistemi
                </Typography>

                <Typography color="text.secondary">
                  React frontend altyapısı başarıyla hazırlandı.
                </Typography>
              </Stack>

              <Button variant="contained">
                Yönetim Paneline Geç
              </Button>
            </Stack>
          </Paper>
        </Container>
      </Box>
  );
}

export default App;