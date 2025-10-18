import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchIngestionSettings, updateIngestionSettings } from '@/modules/admin/services/ingestionService';
import type { IngestionSettings } from '@/modules/admin/types';

export function useIngestionSettings() {
  const queryClient = useQueryClient();
  const query = useQuery<IngestionSettings>({
    queryKey: ['admin', 'ingestion-settings'],
    queryFn: fetchIngestionSettings,
  });

  const update = useMutation({
    mutationFn: updateIngestionSettings,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'ingestion-settings'] });
    },
  });

  return {
    query,
    update,
  };
}
