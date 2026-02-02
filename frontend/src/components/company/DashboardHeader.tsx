import { useState } from "react";
import { Building2, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { JobRegistrationModal } from "@/components/company/JobRegistrationModal";

interface DashboardHeaderProps {
  /** 채용공고 등록/수정 성공 시 호출 (목록 새로고침용) */
  onJobCreated?: () => void;
  /** 수정 모드: 채용공고 jobUid 지정 시 모달을 수정 모드로 연다 */
  editJobUid?: string | null;
  /** 수정 모달 닫을 때 호출 (editJobUid 초기화용) */
  onClearEdit?: () => void;
  /** 기업명 (대시보드 표시) */
  companyName?: string | null;
  /** 회원 이름 (대시보드 표시) */
  memberName?: string | null;
}

export function DashboardHeader({
  onJobCreated,
  editJobUid,
  onClearEdit,
  companyName,
  memberName,
}: DashboardHeaderProps) {
  const [isModalOpen, setIsModalOpen] = useState(false);

  const isEditOpen = !!editJobUid;
  const modalOpen = isModalOpen || isEditOpen;

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      setIsModalOpen(false);
      onClearEdit?.();
    } else {
      setIsModalOpen(open);
    }
  };

  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div className="flex items-center gap-3">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
          <Building2 className="h-6 w-6 text-primary" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-foreground">기업 대시보드</h1>
          <p className="text-muted-foreground">
            {companyName || memberName ? (
              <>
                {companyName && <span className="font-medium text-foreground">{companyName}</span>}
                {companyName && memberName && " · "}
                {memberName && <span>{memberName}</span>}
              </>
            ) : (
              "채용공고와 지원자를 효율적으로 관리하세요"
            )}
          </p>
        </div>
      </div>
      <Button className="btn-gradient-primary" onClick={() => setIsModalOpen(true)}>
        <Plus className="h-4 w-4 mr-2" />
        새 채용공고 등록
      </Button>

      <JobRegistrationModal
        open={modalOpen}
        onOpenChange={handleOpenChange}
        onSuccess={onJobCreated}
        editJobUid={editJobUid ?? undefined}
      />
    </div>
  );
}

