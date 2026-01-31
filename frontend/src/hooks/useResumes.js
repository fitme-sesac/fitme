import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getMyResumes,
  getResume,
  createResume,
  updateResume,
  deleteResume,
  setPrimaryResume,
  setResumeVisibility,
  requestResumeSummary,
  uploadResumeFile,
  deleteResumeAttachment,
} from "@/api/resumes";

/**
 * 내 이력서 목록 조회 hook (로그인 필요)
 */
export function useMyResumes() {
  return useQuery({
    queryKey: ["myResumes"],
    queryFn: getMyResumes,
  });
}

/**
 * 이력서 상세 조회 hook (로그인 필요)
 * @param {number} resumeId - 이력서 ID
 */
export function useResume(resumeId) {
  return useQuery({
    queryKey: ["resume", resumeId],
    queryFn: () => getResume(resumeId),
    enabled: !!resumeId,
  });
}

/**
 * 이력서 생성 mutation hook
 */
export function useCreateResume() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createResume,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myResumes"] });
    },
  });
}

/**
 * 이력서 수정 mutation hook
 */
export function useUpdateResume() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ resumeId, resume }) => updateResume(resumeId, resume),
    onSuccess: (_, { resumeId }) => {
      queryClient.invalidateQueries({ queryKey: ["myResumes"] });
      queryClient.invalidateQueries({ queryKey: ["resume", resumeId] });
    },
  });
}

/**
 * 이력서 삭제 mutation hook
 */
export function useDeleteResume() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteResume,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myResumes"] });
    },
  });
}

/**
 * 대표 이력서 설정 mutation hook
 */
export function useSetPrimaryResume() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: setPrimaryResume,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myResumes"] });
    },
  });
}

/**
 * 이력서 공개 설정 mutation hook
 */
export function useSetResumeVisibility() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ resumeId, isPublic }) =>
      setResumeVisibility(resumeId, isPublic),
    onSuccess: (_, { resumeId }) => {
      queryClient.invalidateQueries({ queryKey: ["myResumes"] });
      queryClient.invalidateQueries({ queryKey: ["resume", resumeId] });
    },
  });
}

/**
 * AI 이력서 요약 요청 mutation hook
 */
export function useRequestResumeSummary() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: requestResumeSummary,
    onSuccess: (_, resumeId) => {
      queryClient.invalidateQueries({ queryKey: ["resume", resumeId] });
    },
  });
}

/**
 * 이력서 파일 업로드 mutation hook
 */
export function useUploadResumeFile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ resumeId, file }) => uploadResumeFile(resumeId, file),
    onSuccess: (_, { resumeId }) => {
      queryClient.invalidateQueries({ queryKey: ["resume", resumeId] });
    },
  });
}

/**
 * 이력서 첨부파일 삭제 mutation hook
 */
export function useDeleteResumeAttachment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ resumeId, attachmentId }) =>
      deleteResumeAttachment(resumeId, attachmentId),
    onSuccess: (_, { resumeId }) => {
      queryClient.invalidateQueries({ queryKey: ["resume", resumeId] });
    },
  });
}
