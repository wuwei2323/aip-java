-- Optional endpoints intentionally remain NULL for historical, manually managed clients.
ALTER TABLE sys_application
    ADD COLUMN post_logout_redirect_uri varchar(500),
    ADD COLUMN backchannel_logout_uri varchar(500),
    ADD COLUMN sso_sync_status varchar(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN sso_sync_error varchar(500);

COMMENT ON COLUMN sys_application.post_logout_redirect_uri IS 'Post logout redirect URI: NULL unowned, empty explicitly denied';
COMMENT ON COLUMN sys_application.backchannel_logout_uri IS 'Backchannel logout URI: NULL unowned, empty explicitly disabled';
COMMENT ON COLUMN sys_application.sso_sync_status IS 'Latest platform configuration sync: PENDING, SUCCESS, FAILED';
COMMENT ON COLUMN sys_application.sso_sync_error IS 'Sanitized localized configuration sync error';
