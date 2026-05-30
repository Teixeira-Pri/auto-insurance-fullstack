import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'sonner';
import InsuranceForm from './components/InsuranceForm';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <div className="App">
        <InsuranceForm />
        <Toaster position="top-right" richColors />
      </div>
    </QueryClientProvider>
  );
}

export default App;
