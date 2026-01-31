-- =========================================================
-- Migration: add missing updated_at columns for tables where set_updated_at trigger exists
-- Purpose: fix runtime errors like: record "NEW" has no field "updated_at"
-- Safe to run multiple times.
-- =========================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

DO $$
DECLARE
    t text;
BEGIN
    FOR t IN SELECT unnest(ARRAY[
        'member', 'employer', 'product', 'orders', 'payment', 'wallet',
        'subscription', 'job_posting', 'resume', 'job_application',
        'interview_schedule', 'resume_project', 'resume_career',
        'resume_certificate', 'inquiry', 'notice', 'faq', 'ad_campaign',
        'outbox_event', 'wallet_credit_lot',
        'phone_verification', 'member_login_log', 'employer_member',
        'subscription_billing_cycle', 'interview_response', 'resume_profile',
        'resume_attachment', 'resume_link', 'job_scrap', 'job_view_log',
        'ad_click_event', 'inquiry_message', 'notice_attachment',
        'notice_delivery', 'report', 'moderation_action', 'member_penalty_point',
        'notification_template', 'payment_cancel'
    ])
    LOOP
        -- table exists?
        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema='public' AND table_name=t
        ) THEN
            -- updated_at missing?
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.columns
                WHERE table_schema='public' AND table_name=t AND column_name='updated_at'
            ) THEN
                EXECUTE format('ALTER TABLE public.%I ADD COLUMN updated_at timestamptz(3) NOT NULL DEFAULT now()', t);
            END IF;
        END IF;
    END LOOP;
END $$;

-- (Optional) re-create the trigger only for phone_verification if missing
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='phone_verification')
       AND NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_set_updated_at_phone_verification') THEN
        EXECUTE 'CREATE TRIGGER trg_set_updated_at_phone_verification
                 BEFORE UPDATE ON public.phone_verification
                 FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
    END IF;
END $$;
