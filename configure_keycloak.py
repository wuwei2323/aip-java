import json
import urllib.request
import urllib.parse
import urllib.error

BASE_URL = "http://192.168.43.100:8080"
ADMIN_USER = "admin"
ADMIN_PASS = "Admin#Keycloak2026"
REALM_NAME = "aip"
INTERNAL_API_URL = "http://192.168.43.1:8080"
SHARED_SECRET = "aip-sso-internal-secret-token"
PORTAL_CLIENT_ID = "000000"
PORTAL_CLIENT_SECRET = "aip-portal-secret-2026"
ADMIN_SVC_CLIENT_ID = "aip-admin-svc"
ADMIN_SVC_CLIENT_SECRET = "aip-admin-svc-secret-2026"

_token = None

def get_admin_token(force_refresh=False):
    global _token
    if _token and not force_refresh:
        return _token
    url = f"{BASE_URL}/realms/master/protocol/openid-connect/token"
    data = urllib.parse.urlencode({
        "client_id": "admin-cli",
        "username": ADMIN_USER,
        "password": ADMIN_PASS,
        "grant_type": "password"
    }).encode("utf-8")
    req = urllib.request.Request(url, data=data, method="POST")
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        _token = res["access_token"]
        return _token

def api_request(method, path, body=None):
    token = get_admin_token()
    url = f"{BASE_URL}{path}"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            content = resp.read().decode("utf-8")
            if content:
                try:
                    return json.loads(content)
                except Exception:
                    return content
            return None
    except urllib.error.HTTPError as e:
        if e.code == 401:
            print(f"Token expired, refreshing token and retrying {method} {path}...")
            token = get_admin_token(force_refresh=True)
            headers["Authorization"] = f"Bearer {token}"
            req = urllib.request.Request(url, data=data, headers=headers, method=method)
            with urllib.request.urlopen(req) as resp:
                content = resp.read().decode("utf-8")
                if content:
                    try:
                        return json.loads(content)
                    except Exception:
                        return content
                return None
        err_msg = e.read().decode("utf-8")
        print(f"HTTPError {e.code} on {method} {path}: {err_msg}")
        raise

def main():
    print("1. Getting admin token...")
    get_admin_token(force_refresh=True)
    print("Admin token acquired successfully.")

    # 2. Check or create realm
    print(f"2. Checking realm '{REALM_NAME}'...")
    try:
        realm = api_request("GET", f"/admin/realms/{REALM_NAME}")
        print(f"Realm '{REALM_NAME}' already exists.")
    except urllib.error.HTTPError as e:
        if e.code == 404:
            print(f"Creating realm '{REALM_NAME}'...")
            api_request("POST", "/admin/realms", {
                "realm": REALM_NAME,
                "enabled": True,
                "displayName": "AIP 集成平台认证中心",
                "accessTokenLifespan": 1800,
                "ssoSessionIdleTimeout": 1800,
                "ssoSessionMaxLifespan": 36000
            })
            realm = api_request("GET", f"/admin/realms/{REALM_NAME}")
            print(f"Realm '{REALM_NAME}' created.")
        else:
            raise

    realm_id = realm["id"]
    print(f"Realm '{REALM_NAME}' UUID: {realm_id}")

    # 2.1 Disable redundant required actions for federated read-only users
    for action_alias in ["VERIFY_PROFILE", "UPDATE_PROFILE", "VERIFY_EMAIL"]:
        try:
            act = api_request("GET", f"/admin/realms/{REALM_NAME}/authentication/required-actions/{action_alias}")
            if act and (act.get("enabled") or act.get("defaultAction")):
                act["enabled"] = False
                act["defaultAction"] = False
                api_request("PUT", f"/admin/realms/{REALM_NAME}/authentication/required-actions/{action_alias}", act)
                print(f"Disabled required action '{action_alias}'.")
        except Exception as e:
            print(f"Could not disable {action_alias}: {e}")

    # 3. Configure User Storage SPI
    print("3. Checking User Storage SPI component...")
    components = api_request("GET", f"/admin/realms/{REALM_NAME}/components?type=org.keycloak.storage.UserStorageProvider")
    aip_storage = next((c for c in components if c.get("providerId") == "aip-user-storage"), None)

    storage_config = {
        "apiUrl": [INTERNAL_API_URL],
        "sharedSecret": [SHARED_SECRET],
        "connectTimeout": ["3000"],
        "readTimeout": ["5000"],
        "cachePolicy": ["NO_CACHE"]
    }

    if not aip_storage:
        print("Creating User Storage SPI component 'aip-user-storage'...")
        api_request("POST", f"/admin/realms/{REALM_NAME}/components", {
            "name": "aip-user-storage",
            "providerId": "aip-user-storage",
            "providerType": "org.keycloak.storage.UserStorageProvider",
            "parentId": realm_id,
            "config": storage_config
        })
        print("User Storage SPI component created.")
    else:
        print("Updating existing User Storage SPI component...")
        aip_storage["parentId"] = realm_id
        aip_storage["config"] = storage_config
        api_request("PUT", f"/admin/realms/{REALM_NAME}/components/{aip_storage['id']}", aip_storage)
        print("User Storage SPI component updated.")

    # 4. Check or create Client 000000 (Portal)
    print(f"4. Checking client '{PORTAL_CLIENT_ID}'...")
    clients = api_request("GET", f"/admin/realms/{REALM_NAME}/clients?clientId={PORTAL_CLIENT_ID}")
    portal_client = clients[0] if clients else None

    portal_payload = {
        "clientId": PORTAL_CLIENT_ID,
        "name": "集成平台门户与单点登录客户端",
        "description": "AIP 平台主门户 OIDC 客户端",
        "enabled": True,
        "protocol": "openid-connect",
        "clientAuthenticatorType": "client-secret",
        "secret": PORTAL_CLIENT_SECRET,
        "publicClient": False,
        "standardFlowEnabled": True,
        "implicitFlowEnabled": False,
        "directAccessGrantsEnabled": True,
        "serviceAccountsEnabled": False,
        "rootUrl": "http://localhost:9090",
        "baseUrl": "http://localhost:9090",
        "redirectUris": [
            "http://localhost:8080/auth/sso/callback",
            "http://127.0.0.1:8080/auth/sso/callback",
            "http://192.168.43.1:8080/auth/sso/callback",
            "http://localhost:9090/*",
            "http://127.0.0.1:9090/*",
            "http://localhost:81/*",
            "http://127.0.0.1:81/*"
        ],
        "webOrigins": ["*"],
        "attributes": {
            "post.logout.redirect.uris": "http://localhost:9090/login##http://localhost:9090/*##http://localhost:81/login##http://localhost:81/*##+",
            "backchannel.logout.url": f"{INTERNAL_API_URL}/auth/sso/backchannel-logout",
            "backchannel.logout.session.required": "true",
            "backchannel.logout.revoke.offline.tokens": "false",
            "pkce.code.challenge.method": "S256",
            "access.token.lifespan": "1800"
        }
    }

    if not portal_client:
        print(f"Creating client '{PORTAL_CLIENT_ID}'...")
        api_request("POST", f"/admin/realms/{REALM_NAME}/clients", portal_payload)
        clients = api_request("GET", f"/admin/realms/{REALM_NAME}/clients?clientId={PORTAL_CLIENT_ID}")
        portal_client = clients[0]
        print(f"Client '{PORTAL_CLIENT_ID}' created with ID: {portal_client['id']}")
    else:
        print(f"Updating client '{PORTAL_CLIENT_ID}'...")
        portal_payload["id"] = portal_client["id"]
        api_request("PUT", f"/admin/realms/{REALM_NAME}/clients/{portal_client['id']}", portal_payload)
        print(f"Client '{PORTAL_CLIENT_ID}' updated.")

    # 4.1 Setup Mappers for 000000
    print(f"Setting up Protocol Mappers for client '{PORTAL_CLIENT_ID}'...")
    client_id_uuid = portal_client["id"]
    existing_mappers = api_request("GET", f"/admin/realms/{REALM_NAME}/clients/{client_id_uuid}/protocol-mappers/models")
    existing_mapper_names = {m["name"] for m in existing_mappers}

    mappers_to_add = [
        {
            "name": "employee_no",
            "protocol": "openid-connect",
            "protocolMapper": "oidc-usermodel-attribute-mapper",
            "consentRequired": False,
            "config": {
                "user.attribute": "employee_no",
                "claim.name": "employee_no",
                "jsonType.label": "String",
                "id.token.claim": "true",
                "access.token.claim": "true",
                "userinfo.token.claim": "true"
            }
        },
        {
            "name": "aip_user_id",
            "protocol": "openid-connect",
            "protocolMapper": "oidc-usermodel-attribute-mapper",
            "consentRequired": False,
            "config": {
                "user.attribute": "aip_user_id",
                "claim.name": "aip_user_id",
                "jsonType.label": "String",
                "id.token.claim": "true",
                "access.token.claim": "true",
                "userinfo.token.claim": "true"
            }
        },
        {
            "name": "dept_id",
            "protocol": "openid-connect",
            "protocolMapper": "oidc-usermodel-attribute-mapper",
            "consentRequired": False,
            "config": {
                "user.attribute": "dept_id",
                "claim.name": "dept_id",
                "jsonType.label": "String",
                "id.token.claim": "true",
                "access.token.claim": "true",
                "userinfo.token.claim": "true"
            }
        },
        {
            "name": "dept_name",
            "protocol": "openid-connect",
            "protocolMapper": "oidc-usermodel-attribute-mapper",
            "consentRequired": False,
            "config": {
                "user.attribute": "dept_name",
                "claim.name": "dept_name",
                "jsonType.label": "String",
                "id.token.claim": "true",
                "access.token.claim": "true",
                "userinfo.token.claim": "true"
            }
        },
        {
            "name": "preferred_username",
            "protocol": "openid-connect",
            "protocolMapper": "oidc-usermodel-property-mapper",
            "consentRequired": False,
            "config": {
                "user.attribute": "username",
                "claim.name": "preferred_username",
                "jsonType.label": "String",
                "id.token.claim": "true",
                "access.token.claim": "true",
                "userinfo.token.claim": "true"
            }
        }
    ]

    for mapper in mappers_to_add:
        if mapper["name"] not in existing_mapper_names:
            print(f"Adding mapper '{mapper['name']}'...")
            api_request("POST", f"/admin/realms/{REALM_NAME}/clients/{client_id_uuid}/protocol-mappers/models", mapper)
            print(f"Mapper '{mapper['name']}' added.")

    # 5. Check or create Client aip-admin-svc
    print(f"5. Checking client '{ADMIN_SVC_CLIENT_ID}'...")
    clients = api_request("GET", f"/admin/realms/{REALM_NAME}/clients?clientId={ADMIN_SVC_CLIENT_ID}")
    admin_svc_client = clients[0] if clients else None

    admin_svc_payload = {
        "clientId": ADMIN_SVC_CLIENT_ID,
        "name": "集成平台管理服务账号",
        "description": "AIP 平台后台与 Keycloak REST API 交互专属服务账号",
        "enabled": True,
        "protocol": "openid-connect",
        "clientAuthenticatorType": "client-secret",
        "secret": ADMIN_SVC_CLIENT_SECRET,
        "publicClient": False,
        "standardFlowEnabled": False,
        "implicitFlowEnabled": False,
        "directAccessGrantsEnabled": False,
        "serviceAccountsEnabled": True
    }

    if not admin_svc_client:
        print(f"Creating client '{ADMIN_SVC_CLIENT_ID}'...")
        api_request("POST", f"/admin/realms/{REALM_NAME}/clients", admin_svc_payload)
        clients = api_request("GET", f"/admin/realms/{REALM_NAME}/clients?clientId={ADMIN_SVC_CLIENT_ID}")
        admin_svc_client = clients[0]
        print(f"Client '{ADMIN_SVC_CLIENT_ID}' created with ID: {admin_svc_client['id']}")
    else:
        print(f"Updating client '{ADMIN_SVC_CLIENT_ID}'...")
        admin_svc_payload["id"] = admin_svc_client["id"]
        api_request("PUT", f"/admin/realms/{REALM_NAME}/clients/{admin_svc_client['id']}", admin_svc_payload)
        print(f"Client '{ADMIN_SVC_CLIENT_ID}' updated.")

    # 5.1 Assign Service Account Roles from realm-management
    print("5.1 Configuring Service Account Roles for aip-admin-svc...")
    svc_client_uuid = admin_svc_client["id"]
    svc_user = api_request("GET", f"/admin/realms/{REALM_NAME}/clients/{svc_client_uuid}/service-account-user")
    svc_user_id = svc_user["id"]
    print(f"Service Account User ID: {svc_user_id}")

    # Find realm-management client
    realm_mgmt_clients = api_request("GET", f"/admin/realms/{REALM_NAME}/clients?clientId=realm-management")
    realm_mgmt_uuid = realm_mgmt_clients[0]["id"]

    # Get available roles on realm-management
    mgmt_roles = api_request("GET", f"/admin/realms/{REALM_NAME}/clients/{realm_mgmt_uuid}/roles")
    target_role_names = {"manage-clients", "view-clients", "view-users", "manage-users", "query-users"}
    roles_to_assign = [r for r in mgmt_roles if r["name"] in target_role_names]
    print(f"Assigning roles: {[r['name'] for r in roles_to_assign]}")

    api_request("POST", f"/admin/realms/{REALM_NAME}/users/{svc_user_id}/role-mappings/clients/{realm_mgmt_uuid}", roles_to_assign)
    print("Service Account roles successfully assigned.")

    print("\n=======================================================")
    print("Keycloak Realm 'aip' and Clients configured successfully!")
    print("=======================================================")

if __name__ == "__main__":
    main()
