import { useCallback, useState } from 'react';
import { DocumentValidationResult } from '../types/document';
import { documentService, extractApiErrorMessage } from '../services/api';

export function useDocumentModal() {
  const [selectedDoc, setSelectedDoc] = useState<DocumentValidationResult | null>(null);
  const [modalLoading, setModalLoading] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);

  const openDocumentModal = useCallback(async (id: string) => {
    setModalLoading(true);
    setModalError(null);
    setSelectedDoc(null);
    try {
      const detail = await documentService.getDocumentById(id);
      setSelectedDoc(detail);
    } catch (err) {
      setModalError(extractApiErrorMessage(err));
    } finally {
      setModalLoading(false);
    }
  }, []);

  const closeDocumentModal = useCallback(() => {
    setSelectedDoc(null);
    setModalLoading(false);
  }, []);

  return {
    selectedDoc,
    modalLoading,
    modalError,
    setModalError,
    openDocumentModal,
    closeDocumentModal,
  };
}
