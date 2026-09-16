# UTF-8 Mojibake Report

Files with corrupted Chinese comments / strings.

> **These cannot be recovered automatically.** The original Chinese text was overwritten; auto-decoders find no GBK round-trip path because the on-disk bytes are valid UTF-8 characters (U+951F U+9225 U+60BE U+70AA U+6793, U+FFFD) - not bytes that came from a UTF-8 -> GBK -> UTF-8 trip.

> **Action required:** human review + manual rewrite of comments.

**1355 .java files scanned.**

**247 files contain mojibake characters** (1206 total occurrences).



## scm-approval (4 files)

### `scm-approval/api/src/main/java/com/scmcloud/approval/api/ApprovalDubboService.java` - 4 mojibake chars

  L9 (1): `* <p>鎻愪緵瀹<U+2103>壒鎻愪氦銆佸<U+E178>鎵归<U+20AC>氳繃/椹冲洖銆佺姸鎬佹煡璇<U+3222>瓑鏍稿績鍔熻兘锛屼緵鍏朵粬寰<U+E1BD>湇鍔<U+FFE0><U+20AC>氳繃 RPC 璋冪敤锟?`
  L28 (1): `* @param userId 瀹<U+2103>壒锟絀D`
  L36 (1): `* @param userId 瀹<U+2103>壒锟絀D`
  L45 (1): `* @return 瀹<U+2103>壒淇<U+2103>伅锛屼笉瀛樺湪鏃惰繑锟絥ull`

### `scm-approval/service/src/main/java/com/scmcloud/approval/domain/entity/SysPermissionApproval.java` - 1 mojibake chars

  L17 (1): `* 鏉冮檺鐢宠<U+E1EC>瀹<U+2103>壒锟?`

### `scm-approval/service/src/main/java/com/scmcloud/approval/mapper/SysPermissionApprovalMapper.java` - 1 mojibake chars

  L9 (1): `* 鏉冮檺鐢宠<U+E1EC>瀹<U+2103>壒锟組apper 鎺<U+30E5>彛`

### `scm-approval/service/src/main/java/com/scmcloud/approval/service/ISysPermissionApprovalService.java` - 1 mojibake chars

  L10 (1): `* 鏉冮檺鐢宠<U+E1EC>瀹<U+2103>壒锟芥湇鍔<U+2605>拷`


## scm-audit (5 files)

### `scm-audit/api/src/main/java/com/scmcloud/audit/api/AuditDubboService.java` - 1 mojibake chars

  L10 (1): `* <p>鎻愪緵鎿嶄綔鏃<U+30E5>織璁板綍銆佹棩蹇楁煡璇<U+3222>瓑鏍稿績鍔熻兘锛屼緵鍏朵粬寰<U+E1BD>湇鍔<U+FFE0><U+20AC>氳繃 RPC 璋冪敤锟?`

### `scm-audit/service/src/main/java/com/scmcloud/audit/domain/entity/SysAuditLog.java` - 1 mojibake chars

  L20 (1): `* 鎿嶄綔瀹<U+00A4><U+E178>鏃<U+30E5>織琛<U+3126>寜鏈堝垎锟?`

### `scm-audit/service/src/main/java/com/scmcloud/audit/domain/entity/SysSensitiveOperationLog.java` - 1 mojibake chars

  L17 (1): `* 鏁忔劅鎿嶄綔鏃<U+30E5>織锟?`

### `scm-audit/service/src/main/java/com/scmcloud/audit/mapper/SysAuditLogMapper.java` - 1 mojibake chars

  L8 (1): `* 鎿嶄綔瀹<U+00A4><U+E178>鏃<U+30E5>織锟芥寜鏈堝垎鍖? Mapper 鎺<U+30E5>彛`

### `scm-audit/service/src/main/java/com/scmcloud/audit/mapper/SysSensitiveOperationLogMapper.java` - 1 mojibake chars

  L8 (1): `* 鏁忔劅鎿嶄綔鏃<U+30E5>織锟組apper 鎺<U+30E5>彛`


## scm-auth (10 files)

### `scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnCredentialController.java` - 4 mojibake chars

  L30 (1): `* WebAuthn 鍑<U+E161>瘉绠<U+FF04>悊鎺<U+0443>埗锟?`
  L33 (1): `* 鍙傝<U+20AC>僄oogle Passkey鍜孎IDO2鏈<U+20AC>浣冲疄锟?`
  L128 (1): `// 璁板綍鎴愬姛鐨勮<U+E17B>璇佸皾锟?`
  L135 (1): `// 璁板綍澶辫触鐨勮<U+E17B>璇佸皾锟?`

### `scm-auth/src/main/java/com/scmcloud/auth/domain/dto/WebauthnCredentialConverter.java` - 5 mojibake chars

  L11 (1): `* WebAuthn 鍑<U+E161>瘉杞<U+E101>崲锟?`
  L13 (1): `* 瀹炵幇Entity鍜孌TO涔嬮棿鐨勮浆锟?`
  L14 (1): `* 闅愯棌鏁忔劅淇<U+2103>伅锛屼繚鎶<U+3086>暟鎹<U+E1BC>畨锟?`
  L23 (1): `* 绉婚櫎鏁忔劅淇<U+2103>伅锛堝叕閽<U+30E7>瓑锟?`
  L79 (1): `entity.setSignCount(0L); // 鍒濆<U+E750>璁<U+2103>暟锟?`

### `scm-auth/src/main/java/com/scmcloud/auth/domain/entity/WebauthnCredential.java` - 19 mojibake chars

  L19 (1): `* WebAuthn 鍑<U+E161>瘉瀹炰綋锟?`
  L27 (1): `* - 鍑<U+E161>瘉ID鍏<U+3125>眬鍞<U+E219>竴锛岄槻姝<U+3220>嚟璇佺<U+E76B>锟?`
  L29 (1): `* - 鍏<U+E104>挜闅旂<U+E787>瀛樺偍锛岄檷浣庢硠闇查<U+E5D3>锟?`
  L69 (1): `* 涓氬姟澶栭敭锛屽叧鑱斿埌sys_user锟?`
  L78 (1): `* 瀛樺偍PEM鏍煎紡鐨勫叕閽<U+30EF>紝鐢<U+3124>簬楠岃瘉璁<U+3088>瘉鍣<U+3127><U+E137>锟?`
  ... 14 more lines

### `scm-auth/src/main/java/com/scmcloud/auth/mapper/WebauthnCredentialMapper.java` - 40 mojibake chars

  L13 (1): `* <p>璇<U+30E6>帴鍙<U+FF46>彁渚涗簡瀵筗ebAuthn鍑<U+E161>瘉鏁版嵁鐨勫熀鏈<U+E101>搷浣滐紝鍖呮嫭锟?p>`
  L15 (1): `*   <li>鏌<U+30E8><U+E1D7>鐢<U+3126>埛鐨勬椿璺冨嚟锟?li>`
  L16 (1): `*   <li>鏍规嵁鐢<U+3126>埛 ID鍜屽嚟锟絀D鏌<U+30E8><U+E1D7>鐗瑰畾鍑<U+E161>瘉</li>`
  L18 (1): `*   <li>鏇存柊鍑<U+E161>瘉鍏宠仈鐨勮<U+E195>澶囧悕锟?li>`
  L19 (1): `*   <li>绂佺敤鎴栧垹闄<U+3085>嚟锟?li>`
  ... 34 more lines

### `scm-auth/src/main/java/com/scmcloud/auth/service/ICustomAuthorizationService.java` - 2 mojibake chars

  L21 (1): `* @param client 宸叉敞鍐岀殑瀹<U+3221>埛锟?`
  L23 (1): `* @param authorizedScopes 宸叉巿鏉冪殑浣滅敤鍩熼泦锟?`

### `scm-auth/src/main/java/com/scmcloud/auth/service/ISysAuthService.java` - 3 mojibake chars

  L23 (1): `* @param ipAddress 瀹<U+3221>埛锟絀P鍦板潃`
  L43 (1): `* @param ipAddress 瀹<U+3221>埛锟絀P鍦板潃`
  L52 (1): `* @param reason 寮哄埗鐢<U+3126>埛鐧诲嚭鐨勫師锟?`

### `scm-auth/src/main/java/com/scmcloud/auth/service/IWebauthnCredentialService.java` - 13 mojibake chars

  L30 (1): `* @param username 鐢<U+3126>埛锟?`
  L39 (1): `* 楠岃瘉骞舵敞鍐屽嚟锟?`
  L44 (1): `* - 妫<U+20AC>鏌<U+30E5>嚟璇両D鏄<U+E21A>惁宸插瓨锟?`
  L49 (1): `* @return 娉<U+3125>唽鐨勫嚟锟紻TO`
  L59 (1): `* @param username 鐢<U+3126>埛锟?`
  ... 8 more lines

### `scm-auth/src/main/java/com/scmcloud/auth/service/Impl/CustomAuthorizationServiceImpl.java` - 2 mojibake chars

  L20 (1): `* 瀹炵幇鑷<U+E044>畾涔夋巿鏉冩湇锟?`
  L40 (1): `// 鐢熸垚鎺堟潈锟?`

### `scm-auth/src/main/java/com/scmcloud/auth/webauthn/WebAuthnConfig.java` - 16 mojibake chars

  L19 (1): `* Relying Party ID (閫氬父鏄<U+E21A>煙锟?`
  L29 (1): `* Relying Party Origin (瀹屾暣锟給rigin URL)`
  L40 (1): `* platform - 浠呭钩鍙拌<U+E17B>璇佸櫒 (锟絋ouchID, FaceID)`
  L41 (2): `* cross-platform - 浠呰法骞冲彴璁<U+3088>瘉锟?锟結ubiKey)`
  L42 (1): `* 锟? 涓<U+3088><U+20AC>呴兘鏀<U+E21B>寔`
  ... 10 more lines

### `scm-auth/src/main/java/com/scmcloud/auth/webauthn/WebAuthnValidator.java` - 9 mojibake chars

  L32 (1): `* WebAuthn 楠岃瘉锟?`
  L33 (1): `* 浣跨敤 WebAuthn4J 搴撳疄锟絎3C WebAuthn 鏍囧噯楠岃瘉`
  L58 (1): `* @param clientDataJSON    瀹<U+3221>埛绔<U+E21B>暟锟絁SON (Base64URL)`
  L90 (1): `* @param clientDataJSON    瀹<U+3221>埛绔<U+E21B>暟锟絁SON (Base64URL)`
  L91 (1): `* @param authenticatorData 璁<U+3088>瘉鍣<U+3126>暟锟?Base64URL)`
  ... 4 more lines


## scm-common (79 files)

### `scm-common/cache/src/main/java/com/scmcloud/common/cache/spring/TwoLevelCacheInvalidationListener.java` - 1 mojibake chars

  L25 (1): `// 鏍煎紡: cacheName|key 锟絚acheName|*`

### `scm-common/core/src/main/java/com/scmcloud/common/constant/DataScopeConstants.java` - 7 mojibake chars

  L4 (1): `* 鏁版嵁鏉冮檺鑼冨洿甯搁噺锟?`
  L18 (1): `* 鍙<U+E219>互鏌<U+30E7>湅鎵<U+20AC>鏈夋暟锟?`
  L23 (1): `* 鑷<U+E044>畾涔夋暟鎹<U+E1BD>潈锟?`
  L24 (1): `* 鍙<U+E219>互鏌<U+30E7>湅鎸囧畾閮<U+3129>棬鐨勬暟鎹<U+E1C6>紙閫氳繃 custom_dept_ids 鎸囧畾锟?`
  L29 (1): `* 鏈<U+E104>儴闂<U+3126>暟鎹<U+E1BD>潈锟?`
  ... 2 more lines

### `scm-common/core/src/main/java/com/scmcloud/common/constant/RoleConstants.java` - 16 mojibake chars

  L4 (1): `* 瑙掕壊甯搁噺锟?`
  L5 (1): `* 瀹氫箟绯荤粺涓<U+E160>殑瑙掕壊绫诲瀷鍜岃<U+E757>鑹蹭唬鐮佸父锟?`
  L20 (1): `* 璺<U+3126>墍鏈夌<U+E764>鎴凤紝鍙<U+E045>湁骞冲彴绠<U+FF04>悊鍛樺彲浠<U+30E7><U+E178>锟?`
  L26 (1): `* 绉熸埛鍐呰<U+E757>鑹诧紝绉熸埛绠<U+FF04>悊鍛樺彲浠<U+30E7><U+E178>锟?`
  L30 (1): `// ==================== 瑙掕壊浠<U+FF47>爜锛圫pring Security 瑙掕壊鍚嶇<U+041E>锟?==================`
  ... 11 more lines

### `scm-common/core/src/main/java/com/scmcloud/common/domain/PageResult.java` - 1 mojibake chars

  L13 (1): `* 鍒嗛<U+3009>缁撴灉锟?`

### `scm-common/core/src/main/java/com/scmcloud/common/exception/ServiceException.java` - 1 mojibake chars

  L6 (1): `* 涓氬姟寮傚父锟?`

### `scm-common/core/src/main/java/com/scmcloud/common/response/ResultCode.java` - 4 mojibake chars

  L21 (1): `// 瀹<U+3221>埛绔<U+E21E>敊锟?xx`
  L26 (1): `// 鏈嶅姟绔<U+E21E>敊锟?xx`
  L55 (1): `// 澶氱<U+E764>鎴烽敊锟?3xx`
  L65 (1): `// 鏈嶅姟闂磋皟鐢<U+3129>敊锟?xxx`

### `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantContextHolder.java` - 3 mojibake chars

  L61 (1): `* 鍦<U+3126>寚瀹氱<U+E764>鎴蜂笂涓嬫枃涓<U+E15F>墽琛屾搷锟?`
  L78 (1): `* 绉熸埛涓婁笅鏂囧洖璋冩帴锟?`
  L86 (1): `* 绉熸埛鏈<U+E045>壘鍒板紓锟?`

### `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantFilter.java` - 12 mojibake chars

  L18 (1): `* 绉熸埛杩囨护锟?`
  L25 (1): `* 4. JWT Token 涓<U+E160>殑 tenant_id claim锛堥渶閰嶅悎JWT瑙<U+FF46>瀽锟?`
  L65 (1): `// 娓呯悊 ThreadLocal锛岄伩鍏嶅唴瀛樻硠锟?`
  L77 (1): `* 4. JWT token锛堝<U+E6E7>鏋滃凡閰嶇疆锟?`
  L80 (1): `// 1. 锟絏-Tenant-Id header`
  ... 6 more lines

### `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantInterceptor.java` - 15 mojibake chars

  L34 (1): `* MyBatis 绉熸埛鎷<U+FE3D>埅锟?`
  L35 (2): `* 鑷<U+E044>姩锟絊QL 涓<U+E15F>敞锟絫enant_id 杩囨护鏉<U+2032>欢`
  L37 (1): `* 鍔熻兘锟?`
  L42 (1): `* 鎺掗櫎琛<U+E7D2>細涓嶉渶瑕佺<U+E764>鎴烽殧绂荤殑绯荤粺琛<U+E7D2>紙濡傜<U+E764>鎴疯<U+3003>鏈<U+E103>韩锟?`
  L58 (1): `* 绉熸埛瀛楁<U+E18C>锟?`
  ... 8 more lines

### `scm-common/core/src/main/java/com/scmcloud/common/tenant/quota/QuotaService.java` - 5 mojibake chars

  L14 (1): `* 妫<U+20AC>鏌<U+30E9>厤棰濇槸鍚<U+FE40>厖锟?`
  L24 (1): `* 妫<U+20AC>鏌<U+30E5>苟娑堣<U+20AC>楅厤棰濓紙鍘熷瓙鎿嶄綔锟?`
  L38 (1): `* @param decrement 閲婃斁鐨勯厤棰濇暟锟?`
  L52 (1): `* 閲嶇疆姣忔棩閰嶉<U+E582>锛堣<U+E179>鍗曘<U+20AC>丄PI璋冪敤锟?`
  L53 (1): `* 鐢卞畾鏃朵换鍔<U+2103>瘡鏃<U+30E5>噷鏅<U+3128>皟锟?`

### `scm-common/core/src/main/java/com/scmcloud/common/tenant/quota/QuotaUsage.java` - 3 mojibake chars

  L26 (1): `* 褰撳墠浣跨敤锟?`
  L31 (1): `* 鏈<U+20AC>澶<U+0447>檺锟?`
  L46 (1): `* 鏄<U+E21A>惁宸茶秴锟?`

### `scm-common/core/src/main/java/com/scmcloud/common/tenant/quota/RequireQuotaCheck.java` - 5 mojibake chars

  L6 (1): `* 閰嶉<U+E582>妫<U+20AC>鏌<U+30E6>敞锟?`
  L8 (1): `* 浣跨敤绀轰緥锟?`
  L13 (1): `*     // 濡傛灉閰嶉<U+E582>涓嶈冻锛屾姏锟絈uotaExceededException`
  L31 (1): `* 娑堣<U+20AC>楃殑閰嶉<U+E582>鏁伴噺锛堥粯锟斤拷`
  L37 (1): `* 濡傛灉涓簍rue锛岄渶瑕侀厤锟紷AfterReturning 瀹炵幇`

### `scm-common/core/src/main/java/com/scmcloud/common/util/UUIDv7Util.java` - 7 mojibake chars

  L9 (1): `* UUID v7鐗堟湰宸<U+30E5>叿锟?`
  L10 (1): `* 鎻愪緵鍩轰簬鏃堕棿鎴崇殑UUID v7鐢熸垚鍜岃<U+0412>鏋愬姛锟?`
  L19 (1): `* 鍩轰簬褰撳墠鏃堕棿鎴崇敓鎴愭湁搴忕殑UUID锛岄<U+20AC>傜敤浜庢暟鎹<U+E1BC>簱涓婚敭绛夊満锟?`
  L28 (1): `* 鐢熸垚UUID 瀛楃<U+E0C1>锟?`
  L37 (1): `* 鐢熸垚UUID瀛楃<U+E0C1>涓诧紙鏃犺繛瀛楃<U+E0C1>锟?`
  ... 2 more lines

### `scm-common/data/src/main/java/com/scmcloud/common/dto/permission/ApiPermissionDTO.java` - 3 mojibake chars

  L13 (2): `* 鐢<U+3124>簬鍔<U+3126><U+20AC>佹潈闄愬姞杞斤紝浠呭寘锟紸PI 鏉冮檺鏍<U+FFE0>獙鎵<U+20AC>闇<U+20AC>鐨勬牳蹇冨瓧锟?`
  L32 (1): `* HTTP 鏂规硶 (GET, POST, PUT, DELETE 锟?`

### `scm-common/data/src/main/java/com/scmcloud/common/dto/role/RoleDTO.java` - 1 mojibake chars

  L68 (1): `private Integer userCount; // 鎷<U+30E6>湁璇<U+30E8><U+E757>鑹茬殑鐢<U+3126>埛锟?`

### `scm-common/data/src/main/java/com/scmcloud/common/dto/user/UserInfo.java` - 1 mojibake chars

  L40 (1): `private Set<PermissionDTO> menuTree; // 鑿滃崟锟?`

### `scm-common/data/src/main/java/com/scmcloud/common/mybatisPlus/config/MybatisPlusConfig.java` - 5 mojibake chars

  L24 (1): `* MybatisPlus 閰嶇疆锟?`
  L42 (1): `paginationInterceptor.setMaxLimit(properties.getPaginationMaxLimit()); // 鏈<U+20AC>澶<U+0443>崟椤甸檺鍒舵暟锟?`
  L46 (1): `// 涔愯<U+E747>閿佹彃锟?`
  L49 (1): `// 闃叉<U+E11B>鍏<U+3128><U+3003>鏇存柊涓庡垹闄<U+3086>彃锟?`
  L62 (1): `// 娉<U+3125>唽 UUID绫诲瀷澶勭悊锟?`

### `scm-common/data/src/main/java/com/scmcloud/common/mybatisPlus/context/DataScopeContextHolder.java` - 1 mojibake chars

  L4 (1): `* 鏁版嵁鏉冮檺涓婁笅锟?`

### `scm-common/data/src/main/java/com/scmcloud/common/mybatisPlus/handler/StringArrayTypeHandler.java` - 1 mojibake chars

  L11 (1): `* PostgreSQL TEXT[] 鏁扮粍绫诲瀷澶勭悊锟?`

### `scm-common/data/src/main/java/com/scmcloud/common/mybatisPlus/handler/UUIDTypeHandler.java` - 49 mojibake chars

  L14 (2): `* 浼佷笟锟経UID绫诲瀷澶勭悊锟?`
  L20 (1): `*   <li>Netflix Architecture - 鍙<U+E21D><U+E747>娴嬫<U+20AC><U+0446><U+E195>璁<U+2605>紝寮傚父蹇<U+E0A6><U+20AC>熷<U+3051>锟?li>`
  L21 (2): `*   <li>MyBatis鏈<U+20AC>浣冲疄锟? 鏃犵姸鎬佺嚎绋嬪畨鍏<U+3128><U+E195>锟?li>`
  L24 (1): `* <p>鎬<U+0446>兘浼樺寲锟?`
  L26 (1): `*   <li>浣跨敤浣嶈繍绠楁浛浠<U+E588>yteBuffer锛屽噺灏戝<U+E1EE>璞<U+2033>垎锟?li>`
  ... 32 more lines

### `scm-common/data/src/main/java/com/scmcloud/common/mybatisPlus/handler/UuidArrayTypeHandler.java` - 1 mojibake chars

  L12 (1): `* PostgreSQL UUID[] 鏁扮粍绫诲瀷澶勭悊锟?`

### `scm-common/data/src/main/java/com/scmcloud/common/mybatisPlus/properties/MybatisPlusProperties.java` - 1 mojibake chars

  L12 (1): `* 鏈<U+20AC>澶<U+0443>崟椤甸檺鍒舵暟閲忥紝榛樿<U+E17B> 1000锟?`

### `scm-common/data/src/main/java/com/scmcloud/common/mybatisPlus/service/DataPermissionService.java` - 1 mojibake chars

  L17 (1): `* 锟絪ys_role_dept 琛<U+3126>煡璇<U+3222>敤鎴烽<U+20AC>氳繃瑙掕壊鑾峰緱鐨勫彲璁块棶閮<U+3129>棬`

### `scm-common/data/src/main/java/com/scmcloud/common/mybatisPlus/service/DefaultDataPermissionService.java` - 2 mojibake chars

  L29 (1): `* SQL: 閫氳繃鐢<U+3126>埛ID -> 鐢<U+3126>埛瑙掕壊 -> 瑙掕壊閮<U+3129>棬鍏宠仈 鑾峰彇鍙<U+E21D><U+E196>闂<U+E1C0>儴锟?`
  L63 (1): `* 妫<U+20AC>鏌<U+30E7>敤鎴锋槸鍚<U+FE3D>湁鑷<U+E044>畾涔夋暟鎹<U+E1BD>潈闄愰厤锟?`

### `scm-common/integration/src/main/java/com/scmcloud/common/integration/sync/config/DataSyncAutoConfiguration.java` - 1 mojibake chars

  L87 (1): `// 鑷<U+E044>姩娉<U+3125>唽鎵<U+20AC>锟紻ataSyncHandler`

### `scm-common/integration/src/main/java/com/scmcloud/common/integration/sync/config/DataSyncProperties.java` - 9 mojibake chars

  L45 (1): `* 娑堣垂鑰呴厤锟?`
  L77 (1): `* 骞跺彂娑堣垂鑰呮暟锟?`
  L100 (1): `* 鏈<U+20AC>澶<U+0447>噸璇曟<U+E0BC>锟?`
  L105 (1): `* 鍒濆<U+E750>閫<U+20AC>閬块棿闅旓紙姣<U+E0A4><U+E757>锟?`
  L110 (1): `* 鏈<U+20AC>澶<U+0447><U+20AC><U+20AC>閬块棿闅旓紙姣<U+E0A4><U+E757>锟?`
  ... 4 more lines

### `scm-common/integration/src/main/java/com/scmcloud/common/integration/sync/consumer/IdempotentChecker.java` - 3 mojibake chars

  L13 (1): `* 鍩轰簬 Redis 瀹炵幇娑堟伅鍘婚噸锛岄槻姝<U+3224>噸澶嶆秷锟?`
  L25 (1): `* 灏濊瘯鑾峰彇澶勭悊锟?`
  L84 (1): `* @return true 濡傛灉宸插<U+E629>锟?`

### `scm-common/integration/src/main/java/com/scmcloud/common/integration/sync/event/DataSyncEvent.java` - 12 mojibake chars

  L13 (1): `* 鏁版嵁鍚屾<U+E11E>浜嬩欢锛堝叏灞<U+20AC>閫氱敤锟?`
  L18 (1): `* - 瀛楄妭璺冲姩鏁版嵁鍚屾<U+E11E>涓<U+E162>棿锟?`
  L45 (1): `* 浜嬩欢鐗堟湰锛堜箰瑙傞攣锟?`
  L50 (1): `* 婧愭湇鍔<U+2033>悕锟?`
  L60 (1): `* 婧愯<U+3003>锟?`
  ... 7 more lines

### `scm-common/integration/src/main/java/com/scmcloud/common/integration/sync/handler/DataSyncHandler.java` - 6 mojibake chars

  L7 (1): `* 鏁版嵁鍚屾<U+E11E>澶勭悊鍣<U+3126>帴锟?`
  L16 (1): `* 鑾峰彇澶勭悊鐨勮仛鍚堢被锟?`
  L18 (1): `* @return 鑱氬悎绫诲瀷锛堝<U+E6E7> User, Dept, Role锟?`
  L26 (1): `* @throws DataSyncException 澶勭悊澶辫触鏃舵姏锟?`
  L31 (1): `* 鍏<U+3129>噺鍚屾<U+E11E>锛堝<U+E1EE>璐<U+FE3F>慨澶嶆椂璋冪敤锟?`
  ... 1 more lines

### `scm-common/integration/src/main/java/com/scmcloud/common/integration/sync/publisher/DataSyncPublisher.java` - 4 mojibake chars

  L8 (1): `* 鏁版嵁鍚屾<U+E11E>浜嬩欢鍙戝竷鍣<U+3126>帴锟?`
  L23 (1): `* 寮傛<U+E11E>鍙戝竷浜嬩欢锛坒ire-and-forget锟?`
  L25 (1): `* 鍐呴儴浣跨敤 CompletableFuture 澶勭悊鍥炶皟锛屼絾涓嶆毚闇茬粰璋冪敤锟?`
  L40 (1): `* 鍙戝竷鍒版<U+E134>淇<U+FFE0>槦锟?`

### `scm-common/integration/src/main/java/com/scmcloud/common/integration/sync/publisher/KafkaDataSyncPublisher.java` - 2 mojibake chars

  L20 (1): `* Kafka 鏁版嵁鍚屾<U+E11E>浜嬩欢鍙戝竷锟?`
  L23 (1): `* - 鍒嗗竷寮忚拷韪<U+E048>泦鎴愶紙OpenTelemetry锟?`

### `scm-common/integration/src/main/java/com/scmcloud/common/seata/config/SeataAutoConfiguration.java` - 5 mojibake chars

  L10 (1): `* Seata 鑷<U+E044>姩閰嶇疆锟?`
  L12 (2): `* <p>涓哄井鏈嶅姟鎻愪緵鍒嗗竷寮忎簨鍔<U+00A4>兘鍔涖<U+20AC>傛敮锟紸T銆乀CC銆丼AGA銆乆A 妯<U+2033>紡锟?`
  L14 (1): `* <p>浣跨敤鏂瑰紡锟?`
  L36 (1): `* 鍏<U+3125>眬浜嬪姟鎵<U+E0A3>弿锟?`

### `scm-common/monitoring/src/main/java/com/scmcloud/common/metrics/BusinessMetrics.java` - 7 mojibake chars

  L17 (1): `* 鑷<U+E044>畾涔夋寚锟?`
  L35 (1): `* 璁板綍涓氬姟鎸囨爣 - 璁<U+2103>暟锟?`
  L36 (1): `* 绀轰緥锛氱櫥褰曟<U+E0BC>鏁般<U+20AC>佽<U+E179>鍗曟暟閲忋<U+20AC>佹敮浠樻<U+E0BC>锟?`
  L49 (1): `* 璁板綍涓氬姟鎸囨爣 - 璁<U+2103>椂锟?`
  L50 (1): `* 绀轰緥锛氭帴鍙<U+FF48><U+20AC>楁椂銆佷笟鍔<U+2033><U+E629>鐞嗘椂锟?`
  ... 2 more lines

### `scm-common/monitoring/src/main/java/com/scmcloud/common/sentinel/annotation/RateLimit.java` - 3 mojibake chars

  L28 (2): `* 闄愭祦绫诲瀷锟?QPS 2-绾跨<U+25BC>锟?`
  L33 (1): `* 鏄<U+E21A>惁寮<U+20AC>鍚<U+E21E>泦缇<U+3089>檺锟?`

### `scm-common/monitoring/src/main/java/com/scmcloud/common/sentinel/exception/SentinelExceptionHandlerStrategy.java` - 2 mojibake chars

  L7 (1): `* Sentinel 寮傚父澶勭悊鍣<U+3127>瓥鐣<U+30E6>帴锟?`
  L15 (1): `* 鍒<U+3086>柇鏄<U+E21A>惁鏀<U+E21B>寔澶勭悊璇<U+30E5>紓锟?`

### `scm-common/monitoring/src/main/java/com/scmcloud/common/trace/annotation/BusinessTrace.java` - 1 mojibake chars

  L28 (1): `* 鏄<U+E21A>惁璁板綍杩斿洖锟?`

### `scm-common/security/core/src/main/java/com/scmcloud/common/web/domain/SecurityUser.java` - 2 mojibake chars

  L23 (1): `* 鑷<U+E044>畾锟経serDetails`
  L63 (1): `// 鍚堝苟瑙掕壊鍜屾潈闄愶紝闃插尽绌洪泦锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/log/annotation/AuditLog.java` - 3 mojibake chars

  L28 (3): `* 椋庨櫓绛夌骇: 1-锟?2-锟?3-锟?4-鏋侀珮`

### `scm-common/web/src/main/java/com/scmcloud/common/log/enums/SecurityEventType.java` - 3 mojibake chars

  L39 (3): `private final Integer riskLevel; // 1-锟?-锟?-锟?-涓<U+30E9>噸 5-绱<U+044D>拷`

### `scm-common/web/src/main/java/com/scmcloud/common/log/interceptor/LogInterceptor.java` - 2 mojibake chars

  L34 (1): `// 璁剧疆鐢<U+3126>埛涓婁笅锟?`
  L44 (1): `// 璁板綍璇锋眰寮<U+20AC>锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/log/service/ISysAuditLogService.java` - 7 mojibake chars

  L7 (1): `* 鎿嶄綔瀹<U+00A4><U+E178>鏃<U+30E5>織锟芥湇鍔<U+2605>拷 * </p>`
  L17 (1): `* @param userId 鐢<U+3126>埛鍞<U+E219>竴鏍囪瘑锟?`
  L18 (1): `* @param username 鐢<U+3126>埛锟?`
  L28 (1): `* @param username 鐢<U+3126>埛锟?`
  L37 (1): `* @param userId 鐢<U+3126>埛鍞<U+E219>竴鏍囪瘑锟?`
  ... 2 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/log/util/LogUtils.java` - 5 mojibake chars

  L12 (1): `* 鏃<U+30E5>織宸<U+30E5>叿锟?`
  L26 (1): `* 璁剧疆鐢<U+3126>埛涓婁笅锟?`
  L63 (1): `* 缁撴瀯鍖栨棩锟? 涓氬姟鏃<U+30E5>織`
  L77 (1): `* 缁撴瀯鍖栨棩锟? 鎺<U+30E5>彛璋冪敤`
  L92 (1): `* 缁撴瀯鍖栨棩锟? RPC璋冪敤`

### `scm-common/web/src/main/java/com/scmcloud/common/rest/aspect/HttpExchangeFallbackAspect.java` - 24 mojibake chars

  L21 (1): `* <p>鏇夸唬 OpenFeign 锟紽allbackFactory</p>`
  L23 (1): `* <p>鍔熻兘锟?`
  L25 (2): `*   <li>鎷<U+FE3D>埅鎵<U+20AC>锟紷SentinelResource 娉<U+3128><U+0412>鐨勬柟锟?li>`
  L26 (1): `*   <li>寮傚父鍒嗙被锛欱lockException锛堥檺娴侊級銆乀imeoutException锛堣秴鏃讹級銆佸叾浠栧紓锟?li>`
  L27 (1): `*   <li>鑷<U+E044>姩璋冪敤鎺<U+30E5>彛锟絛efault 闄嶇骇鏂规硶</li>`
  ... 15 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/rest/client/SysAuthServiceClient.java` - 12 mojibake chars

  L16 (1): `* 璁<U+3088>瘉鏈嶅姟瀹<U+3221>埛绔<U+E224>紙@HttpExchange 鐗堟湰锟?`
  L17 (1): `* <p>鏇夸唬 OpenFeign 锟絊ysAuthServiceClient</p>`
  L35 (1): `* <p>浣跨敤鍦烘櫙锟?`
  L37 (1): `*   <li>绠<U+FF04>悊鍛樺己鍒剁敤鎴蜂笅锟?li>`
  L42 (2): `* <p>闄嶇骇绛栫暐锛氳繑锟?03 鏈嶅姟涓嶅彲锟?p>`
  ... 5 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/rest/client/SysUserServiceClient.java` - 11 mojibake chars

  L15 (1): `* 鐢<U+3126>埛鏈嶅姟瀹<U+3221>埛绔<U+E224>紙@HttpExchange 鐗堟湰锟?`
  L16 (1): `* <p>鏇夸唬 OpenFeign 锟絊ysUserServiceClient</p>`
  L18 (1): `* <p>鏋舵瀯璇存槑锟?`
  L24 (2): `* <p>姝<U+3085><U+E179>鎴风<U+E06C>锟絪ystem-service 锟絊ysUserController 绔<U+E21C>偣瀵瑰簲`
  L26 (1): `* <p>娉<U+3126>剰锛氳<U+E17B>璇佺浉鍏虫柟娉曪紙getUserByUsername, getUserRoles, getUserPermissions锟?`
  ... 4 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/rest/config/LoadBalancerConfiguration.java` - 25 mojibake chars

  L23 (1): `*   <li><b>weighted-round-robin</b>: 鍔犳潈杞<U+E1BF><U+E1D7>锛屾牴鎹<U+E1BC>疄渚嬫潈閲嶅垎閰嶈<U+E1EC>锟?li>`
  L26 (1): `* <p>閰嶇疆鏂瑰紡锛坅pplication.yml锟?`
  L34 (1): `*       # Nacos 鏉冮噸閰嶇疆锛堜粎 weighted-round-robin 绛栫暐鐢熸晥锟?`
  L39 (1): `* <p>Nacos 瀹炰緥鏉冮噸閰嶇疆绀轰緥锟?`
  L41 (2): `* # 锟絅acos 鎺<U+0443>埗鍙伴厤缃<U+E1BC>疄锟絤etadata`
  ... 15 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/rest/config/RestClientHttpExchangeConfig.java` - 19 mojibake chars

  L25 (1): `* <p>鍔熻兘锟?`
  L27 (1): `*   <li>鍒涘缓 RestClient Bean锛堝寘锟絤TLS + 绛惧悕鎷<U+FE3D>埅鍣<U+E7D2>級</li>`
  L28 (2): `*   <li>鍒涘缓 HttpServiceProxyFactory锛堢敤锟絳@code @HttpExchange} 浠<U+FF47>悊锟?li>`
  L29 (1): `*   <li>闆嗘垚 Nacos 鏈嶅姟鍙戠幇锛堝姩鎬佽<U+0412>鏋愭湇鍔<U+2033>湴鍧<U+20AC>锟?li>`
  L30 (1): `*   <li>娉<U+3125>唽 3 涓<U+E044><U+E179>鎴风<U+E06C> Bean锛圲serServiceClient銆丄uthServiceClient銆丳ermissionServiceClient锟?li>`
  ... 10 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/rest/config/RestClientMtlsConfig.java` - 10 mojibake chars

  L28 (1): `* 鏇夸唬 OpenFeign 锟紽eignMtlsConfig`
  L35 (1): `*   <li>闆跺仠鏈鸿瘉涔<U+FE3E>儹鏇存柊锛堥<U+20AC>氳繃 CertificateReloaderRestClient锟?li>`
  L72 (1): `* <p>浣跨敤 Spring Boot 4.0 锟絊SL Bundle 鏈哄埗锛屾敮鎸佽瘉涔<U+FE3E>儹鏇存柊</p>`
  L78 (1): `// 鍔犺浇 KeyStore 锟絋rustStore`
  L109 (1): `* 閰嶇疆 ClientHttpRequestFactory锛圧estClient 浣跨敤锟?`
  ... 5 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/rest/config/SentinelRestClientConfiguration.java` - 34 mojibake chars

  L17 (1): `* <p>鏇夸唬 OpenFeign 锟絊entinel 闆嗘垚</p>`
  L19 (1): `* <p>鍔熻兘锟?`
  L23 (2): `*   <li>涓嶄緷锟紽eign锛屼娇鐢<U+3129><U+20AC>氱敤锟紷SentinelResource 娉<U+3128><U+0412></li>`
  L36 (1): `* 鍒濆<U+E750>锟絊entinel 瑙勫垯`
  L46 (1): `* 鍒濆<U+E750>鍖栭檺娴佽<U+E749>锟?`
  ... 24 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/rest/interceptor/RestClientRequestSignatureInterceptor.java` - 16 mojibake chars

  L21 (1): `* RestClient 璇锋眰绛惧悕鎷<U+FE3D>埅锟?`
  L22 (1): `* <p>鏇夸唬 OpenFeign 锟紽eignRequestSignatureInterceptor</p>`
  L24 (1): `* <p>鍔熻兘锟?`
  L26 (1): `*   <li>鑷<U+E044>姩涓烘墍锟紿TTP 璇锋眰娣诲姞 HMAC-SHA256 绛惧悕</li>`
  L27 (1): `*   <li>闃查噸鏀炬敾鍑伙細浣跨敤鏃堕棿锟? nonce</li>`
  ... 11 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/rest/reload/CertificateReloaderRestClient.java` - 23 mojibake chars

  L24 (1): `* <p>鏀<U+E21B>寔闆跺仠锟絤TLS 璇佷功鏇存柊</p>`
  L26 (1): `* <p>鍔熻兘锟?`
  L29 (1): `*   <li>鑷<U+E044>姩閲嶆柊鍔犺浇璇佷功锛堟棤闇<U+20AC>閲嶅惎搴旂敤锟?li>`
  L30 (1): `*   <li>绾跨<U+25BC>瀹夊叏锟絊SLContext 鏇存柊</li>`
  L47 (1): `// 褰撳墠 SSLContext锛堢嚎绋嬪畨鍏<U+3127>殑鍘熷瓙寮曠敤锟?`
  ... 18 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/security/annotation/EncryptField.java` - 1 mojibake chars

  L18 (1): `* 鍔犲瘑绠楁硶锛堥<U+E569>鐣欙紝褰撳墠浠呮敮鎸丄ES锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/config/JacksonConfig.java` - 1 mojibake chars

  L26 (1): `// 娉<U+3125>唽JavaTimeModule浠<U+30E6>敮鎸丣ava 8鏃堕棿绫诲瀷搴忓垪锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/config/PkceAuthorizationCodeTokenResponseClient.java` - 3 mojibake chars

  L45 (1): `// 2锔忊儯 鏍<U+FFE0>獙 PKCE 鎸戞垬锟?`
  L66 (2): `* 璁<U+FF04>畻 PKCE 锟絊HA-256 challenge 鍊硷紙Base64Url 缂栫爜锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/config/SecurityConfig.java` - 9 mojibake chars

  L34 (1): `* SpringSecurity 閰嶇疆锟?`
  L54 (1): `* Spring Security 涓昏繃婊<U+3085>櫒锟?`
  L60 (2): `// 1锔忊儯 绂佺敤 CSRF锛堜娇锟絁WT锟?`
  L66 (1): `// 3锔忊儯 鏃犵姸锟絊ession 绠<U+FF04>悊`
  L72 (1): `// 4锔忊儯 寮傚父澶勭悊锛堣<U+E17B>璇佷笌鎺堟潈锟?`
  ... 3 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/security/crypto/AESEncryptor.java` - 1 mojibake chars

  L14 (1): `* AES 鍔犲瘑宸<U+30E5>叿锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/dto/UserDTOWithSensitive.java` - 4 mojibake chars

  L35 (1): `@EncryptField  // 鏁版嵁搴撳瓨鍌<U+3125>姞锟?`
  L36 (1): `@Sensitive(type = SensitiveType.ID_CARD)  // 鍝嶅簲鑴辨晱锟?0101********1234`
  L52 (1): `@Sensitive(type = SensitiveType.BANK_CARD)  // 鍝嶅簲鑴辨晱锟?22 **** **** 1234`
  L55 (1): `@Sensitive(type = SensitiveType.ADDRESS)  // 鍦板潃鑴辨晱锛氫繚鐣欏墠6锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/filter/CachedBodyHttpServletRequest.java` - 2 mojibake chars

  L14 (1): `* 鍙<U+E21E>噸澶嶈<U+E1F0>鍙栬<U+E1EC>姹備綋鐨勫寘瑁呭櫒锛岀敤浜庡湪杩囨护鍣<U+3124>腑棰勮<U+E1F0> JSON 鍐呭<U+E190>锟?`
  L15 (1): `* 浠嶅厑璁稿悗缁<U+E162>摼璺<U+E21B><U+E11C>甯歌<U+E1F0>鍙栬<U+E1EC>姹備綋锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/filter/JwtAuthenticationFilter.java` - 4 mojibake chars

  L30 (1): `* Jwt 杩囨护锟?`
  L87 (2): `// 璁剧疆锟絊ecurity涓婁笅锟?`
  L108 (1): `// 娓呯悊ThreadLocal缂撳瓨锛岄槻姝<U+3220>唴瀛樻硠锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/filter/SqlInjectionFilter.java` - 4 mojibake chars

  L30 (1): `* SQL/XSS 杞婚噺鍛婅<U+E11F>杩囨护鍣<U+E7D2>細榛樿<U+E17B>浠呭憡璀<U+FE3C>紝渚濊禆鎸佷箙灞傚弬鏁板寲闃叉姢锟?`
  L143 (2): `// 闈炰弗锟絁SON 鎴栬<U+0412>鏋愬<U+3051>璐<U+30EF>紝璺宠繃 JSON 浣撴壂锟?`
  L218 (1): `// 鍏朵粬绫诲瀷涓嶅<U+E629>锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/handler/JwtAccessDeniedHandler.java` - 1 mojibake chars

  L18 (1): `* Jwt 鎷掔粷澶勭悊锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/handler/JwtAuthenticationEntryPoint.java` - 1 mojibake chars

  L18 (1): `* JWT 璁<U+3088>瘉鍏<U+30E5>彛锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/idempotent/Idempotent.java` - 7 mojibake chars

  L6 (1): `* 骞傜瓑鎬<U+0444>敞锟?`
  L19 (1): `* 骞傜瓑锟絢ey鐨勫墠缂<U+20AC>`
  L24 (1): `* 骞傜瓑鎬<U+E731>ey鐨凷pEL琛<U+3128>揪锟?`
  L25 (1): `* 渚嬪<U+E6E7>: #userId 锟?request.orderId`
  L30 (1): `* 杩囨湡鏃堕棿锛堢<U+E757>锟?`
  ... 2 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/security/idempotent/IdempotentAspect.java` - 3 mojibake chars

  L26 (1): `* 骞傜瓑鎬<U+0443>垏锟?`
  L46 (1): `// 灏濊瘯鑾峰彇锟?`
  L58 (1): `// 濡傛灉涓氬姟鎵<U+0446><U+E511>澶辫触锛屽垹闄<U+3085>箓绛夋<U+20AC><U+E731>ey锛屽厑璁搁噸锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/interceptor/EncryptionInterceptor.java` - 3 mojibake chars

  L23 (1): `*鏁忔劅瀛楁<U+E18C>鍔犲瘑鎷<U+FE3D>埅锟?`
  L51 (1): `// 鎻掑叆/鏇存柊鏃跺姞锟?`
  L68 (1): `// 鏌<U+30E8><U+E1D7>鏃惰<U+0412>锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/properties/JwtProperties.java` - 14 mojibake chars

  L8 (1): `* Jwt 閰嶇疆锟?`
  L20 (1): `* JWT绛惧悕瀵嗛挜锛堣嚦锟?2浣嶏級`
  L32 (1): `* 榛樿<U+E17B>: 7锟?`
  L37 (1): `* Token 绛惧彂锟?`
  L42 (1): `* Token 璇锋眰澶村悕锟?`
  ... 9 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/security/properties/SecurityProperties.java` - 1 mojibake chars

  L8 (1): `* Security 鐨勪竴浜涘熀鏈<U+E104>厤锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/session/SessionManager.java` - 5 mojibake chars

  L68 (1): `// 缁<U+E162>暱褰撳墠 TTL锛屽<U+E6E7>锟絋TL 鍒欏洖閫<U+20AC> 30 鍒嗛挓`
  L103 (1): `* 鍏抽棴鐢<U+3126>埛鐨勬墍鏈変細锟?`
  L121 (1): `* 鑾峰彇鐢<U+3126>埛鐨勬墍鏈変細锟?`
  L162 (1): `* 妫<U+20AC>鏌<U+30E7>敤鎴锋槸鍚<U+FE40>湪锟?`
  L206 (1): `* 鑾峰彇鐢<U+3126>埛鐨勪細璇濈粺璁<U+2032>俊锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/stepup/StepUpFilter.java` - 1 mojibake chars

  L30 (1): `* Step-Up 璁<U+3088>瘉杩囨护鍣<U+E7D2>細鍩轰簬 Sentinel 鐔旀柇淇濇姢鐨勬晱鎰熸搷浣滀簩娆<U+00A4><U+E17B>璇佹牎锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/stepup/StepUpProperties.java` - 3 mojibake chars

  L15 (1): `// 宸<U+30E4>綔鏃堕棿绐楀彛锛堝惈锟?`
  L18 (1): `// 鏄<U+E21A>惁鍚<U+E21C>敤鏂拌<U+E195>澶囪<U+0415>锟?`
  L22 (1): `// 绛栫暐缂撳瓨鍒锋柊绉掓暟锛圱TL锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/util/DesensitizeUtils.java` - 7 mojibake chars

  L4 (1): `* 鏁版嵁鑴辨晱宸<U+30E5>叿锟?`
  L13 (1): `* 鎵嬫満鍙疯劚锟?`
  L24 (1): `* 韬<U+E0A1>唤璇佽劚锟?`
  L51 (1): `* 閾惰<U+E511>鍗<U+00A4>劚锟?`
  L63 (1): `* 锟?`
  ... 2 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/security/util/FilterBypassHelper.java` - 6 mojibake chars

  L12 (1): `* <p>鎻愪緵鍏<U+E100>叡鐨勭櫧鍚嶅崟鍖归厤鍜屾梺璺<U+E21A>垽鏂<U+E162><U+20AC>昏緫锛屼緵澶氫釜瀹夊叏杩囨护鍣<U+3125><U+E632>锟?`
  L24 (1): `* 妫<U+20AC>锟経RI鏄<U+E21A>惁鍖归厤浠绘剰妯<U+2033>紡`
  L27 (1): `* @param patterns 鍖归厤妯<U+2033>紡鍒楄<U+3003>锛堟敮鎸丄nt椋庢牸锟?`
  L51 (1): `// 1. 妫<U+20AC>鏌<U+30E8>矾寰勫尮锟?`
  L66 (1): `// 3. 妫<U+20AC>鏌<U+30E8><U+E757>鑹插尮锟?`
  ... 1 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/security/util/HttpServletRequestUtils.java` - 6 mojibake chars

  L70 (1): `// 璁板綍寮傚父浣嗕笉鍚戜笂鎶涘嚭锛岄伩鍏嶅奖鍝嶈<U+E17B>璇佹祦锟?`
  L79 (1): `* 浣跨敤璇锋眰绾<U+0445>紦瀛橈紝閬垮厤鍦<U+3125>悓涓<U+20AC>璇锋眰涓<U+E162>噸澶嶈<U+E178>锟?`
  L83 (1): `* @return 璁惧<U+E62C>ID锛屼繚璇侀潪绌轰笖闀垮害涓嶈秴锟?8瀛楃<U+E0C1>`
  L95 (1): `// 娓呯悊闈炴硶瀛楃<U+E0C1>锛堝彧淇濈暀瀛楁瘝銆佹暟瀛椼<U+20AC>佹<U+00ED>绾裤<U+20AC>佷笅鍒掔嚎锟?`
  L98 (1): `// 濡傛灉娓呯悊鍚庝负绌猴紝鍒欓噸鏂扮敓锟?`
  ... 1 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/security/util/IpUtils.java` - 3 mojibake chars

  L10 (1): `* IP 宸<U+30E5>叿锟?`
  L23 (1): `* 鑾峰彇瀹<U+3221>埛绔<U+E21C>湡锟絀P`
  L80 (1): `* 鍒<U+3086>柇鏄<U+E21A>惁涓哄唴锟絀P`

### `scm-common/web/src/main/java/com/scmcloud/common/security/util/JwtUtils.java` - 19 mojibake chars

  L24 (1): `* Jwt 宸<U+30E5>叿锟?`
  L79 (1): `* 鐢熸垚璁块棶浠<U+3087>墝锛堝彲鎸囧畾 AMR锟?`
  L96 (1): `// 瀛樺偍 Token鍏冩暟锟?`
  L134 (1): `// 3. 榛戝悕鍗曟<U+E5C5>锟?`
  L145 (1): `// 5. IP楠岃瘉锛堝彲閰嶇疆锟?`
  ... 12 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/security/util/PasswordUtils.java` - 5 mojibake chars

  L7 (1): `* 瀵嗙爜宸<U+30E5>叿锟?`
  L35 (1): `// 纭<U+E1BB>繚鑷冲皯鍖呭惈姣忕<U+E752>绫诲瀷鐨勫瓧锟?`
  L63 (1): `* 1: 锟?`
  L65 (1): `* 3: 锟?`
  L75 (1): `// 闀垮害妫<U+20AC>锟?`

### `scm-common/web/src/main/java/com/scmcloud/common/security/util/TotpUtils.java` - 15 mojibake chars

  L26 (1): `private static final int TIME_STEP = 30; // 30绉掓椂闂寸獥锟?`
  L28 (1): `private static final int WINDOW = 1; // 鍏佽<U+E18F>鍓嶅悗1涓<U+E045>椂闂寸獥鍙<U+FF4F>紙闃叉<U+E11B>鏃堕棿璇<U+E21A>樊锟?`
  L31 (1): `* 鐢熸垚瀵嗛挜锛圔ase32缂栫爜锟?`
  L41 (1): `* 鐢熸垚浜岀淮鐮乁RL锛堢敤浜嶨oogle Authenticator鎵<U+E0A3>弿锟?`
  L44 (1): `* @param issuer 鍙戣<U+E511>鑰咃紙搴旂敤鍚嶇<U+041E>锟?`
  ... 9 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/uaa/config/AuthorizationServerConfig.java` - 9 mojibake chars

  L98 (1): `* 榛樿<U+E17B>瀹夊叏杩囨护锟?`
  L115 (1): `* 娉<U+3125>唽瀹<U+3221>埛锟?`
  L119 (1): `// Web 瀹<U+3221>埛锟?`
  L146 (1): `// 绉诲姩瀹<U+3221>埛绔<U+E224>紙浣跨敤PKCE锟?`
  L149 (1): `.clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // 鍏<U+E100>紑瀹<U+3221>埛锟?`
  ... 4 more lines

### `scm-common/web/src/main/java/com/scmcloud/common/uaa/service/CustomUserDetailsService.java` - 1 mojibake chars

  L35 (1): `throw new UsernameNotFoundException("鐢<U+3126>埛涓嶅瓨锟?" + username);`


## scm-finance (11 files)

### `scm-finance/api/src/main/java/com/scmcloud/finance/api/FinanceDubboService.java` - 4 mojibake chars

  L12 (1): `* <p>鎻愪緵缁撶畻銆佸彂绁<U+3123><U+20AC>佽繍璐硅<U+E178>绠楃瓑鏍稿績鍔熻兘锛屼緵鍏朵粬寰<U+E1BD>湇鍔<U+FFE0><U+20AC>氳繃 RPC 璋冪敤锟?`
  L20 (1): `* 鍒涘缓缁撶畻锟?`
  L23 (1): `* @return 缁撶畻鍗曚俊锟?`
  L31 (1): `* @return 鍙戠<U+30A8>淇<U+2103>伅锛屼笉瀛樺湪鏃惰繑锟絥ull`

### `scm-finance/service/src/main/java/com/scmcloud/finance/domain/entity/FreightRule.java` - 1 mojibake chars

  L20 (1): `* 杩愯垂瑙勫垯锟?`

### `scm-finance/service/src/main/java/com/scmcloud/finance/domain/entity/Invoice.java` - 1 mojibake chars

  L18 (1): `* 鍙戠<U+30A8>锟?`

### `scm-finance/service/src/main/java/com/scmcloud/finance/domain/entity/PlatformServiceFee.java` - 1 mojibake chars

  L18 (1): `* 骞冲彴鏈嶅姟璐硅<U+3003>锛圫aaS骞冲彴锟?`

### `scm-finance/service/src/main/java/com/scmcloud/finance/domain/entity/ReconciliationRecord.java` - 1 mojibake chars

  L20 (1): `* 瀵硅处璁板綍锟?`

### `scm-finance/service/src/main/java/com/scmcloud/finance/domain/entity/SettlementItem.java` - 1 mojibake chars

  L20 (1): `* 缁撶畻鏄庣粏锟?`

### `scm-finance/service/src/main/java/com/scmcloud/finance/mapper/FreightRuleMapper.java` - 1 mojibake chars

  L8 (1): `* 杩愯垂瑙勫垯锟組apper 鎺<U+30E5>彛`

### `scm-finance/service/src/main/java/com/scmcloud/finance/mapper/InvoiceMapper.java` - 1 mojibake chars

  L8 (1): `* 鍙戠<U+30A8>锟組apper 鎺<U+30E5>彛`

### `scm-finance/service/src/main/java/com/scmcloud/finance/mapper/PlatformServiceFeeMapper.java` - 1 mojibake chars

  L8 (1): `* 骞冲彴鏈嶅姟璐硅<U+3003>锛圫aaS骞冲彴锟組apper 鎺<U+30E5>彛`

### `scm-finance/service/src/main/java/com/scmcloud/finance/mapper/ReconciliationRecordMapper.java` - 1 mojibake chars

  L8 (1): `* 瀵硅处璁板綍锟組apper 鎺<U+30E5>彛`

### `scm-finance/service/src/main/java/com/scmcloud/finance/mapper/SettlementItemMapper.java` - 1 mojibake chars

  L8 (1): `* 缁撶畻鏄庣粏锟組apper 鎺<U+30E5>彛`


## scm-gateway (7 files)

### `scm-gateway/src/main/java/com/scmcloud/gateway/config/ApiSignatureConfiguration.java` - 1 mojibake chars

  L19 (1): `* 閬靛惊蹇<U+E0A6><U+20AC>熷<U+3051>璐<U+30E5>師鍒欙細濡傛灉缂哄皯鍏抽敭瀵嗛挜锛屽簲鐢<U+3127><U+25BC>搴忓皢鏃犳硶鍚<U+E21A>姩锟?`

### `scm-gateway/src/main/java/com/scmcloud/gateway/config/SecurityConfigurationValidator.java` - 3 mojibake chars

  L16 (1): `* 瀹夊叏閰嶇疆楠岃瘉锟?`
  L96 (1): `* 楠岃瘉瀵嗙爜寮哄害鏄<U+E21A>惁绗<U+FE40>悎瑕佹眰锟?`
  L120 (1): `* 纭<U+E1BC>畾褰撳墠鐜<U+E21A><U+E568>鏄<U+E21A>惁闇<U+20AC>瑕佷弗鏍肩殑瀹夊叏楠岃瘉锟?`

### `scm-gateway/src/main/java/com/scmcloud/gateway/security/IdentityTokenEncoder.java` - 1 mojibake chars

  L14 (1): `* 瀵圭敤浜庝笅娓搁獙璇佺殑韬<U+E0A1>唤淇<U+2103>伅杞借嵎杩涜<U+E511>绛惧悕锟?`

### `scm-gateway/src/main/java/com/scmcloud/gateway/support/ip/ClientIpResolver.java` - 2 mojibake chars

  L16 (2): `* 鏍规嵁鍙<U+E219>俊浠<U+FF47>悊閰嶇疆瑙<U+FF46>瀽鍑鸿<U+E749>鑼冪殑瀹<U+3221>埛锟絀P 鍦板潃锟?`

### `scm-gateway/src/main/java/com/scmcloud/gateway/support/ip/IpSubnet.java` - 1 mojibake chars

  L13 (1): `* 鏀<U+E21B>寔 IPv4 锟絀Pv6 鐨勬瀬绠<U+20AC> CIDR 鍖归厤鍣<U+E7D2>拷`

### `scm-gateway/src/main/java/com/scmcloud/gateway/util/SignatureAlgorithm.java` - 7 mojibake chars

  L10 (1): `* 鏀<U+E21B>寔鍝嶅簲寮忕紪绋嬫<U+0101>鍨嬶紝閫傜敤浜嶴pring WebFlux鐜<U+E21A><U+E568>锟?`
  L20 (1): `* 鑾峰彇绛惧悕绠楁硶鐗堟湰锟?`
  L22 (1): `* @return 绠楁硶鐗堟湰瀛楃<U+E0C1>锟?`
  L31 (1): `* @param timestamp 鏃堕棿锟?`
  L32 (1): `* @param nonce     闅忔満锟?`
  ... 2 more lines

### `scm-gateway/src/main/java/com/scmcloud/gateway/util/SignatureAlgorithmRegistry.java` - 1 mojibake chars

  L55 (1): `// 鍥為<U+20AC><U+20AC>鍒伴粯璁<U+3087>増锟?`


## scm-inventory (8 files)

### `scm-inventory/api/src/main/java/com/scmcloud/inventory/api/InventoryDubboService.java` - 7 mojibake chars

  L9 (1): `* <p>鎻愪緵搴撳瓨鏌<U+30E8><U+E1D7>銆佹墸鍑忋<U+20AC>侀噴鏀剧瓑鏍稿績鍔熻兘锟?`
  L20 (2): `* <p>姝<U+3086>柟娉曞弬锟絊eata 鍒嗗竷寮忎簨鍔<U+2605>紝鏃犻渶娣诲姞 @GlobalTransactional 娉<U+3128><U+0412>锟?`
  L21 (1): `* <p>閫氳繃 Dubbo RPC 璋冪敤鏃讹紝浼氳嚜鍔<U+3125>姞鍏<U+30E8>皟鐢<U+3126>柟鐨勫叏灞<U+20AC>浜嬪姟锟?`
  L25 (2): `* @param requestId 骞傜瓑鎬<U+0446><U+E1EC>锟絀D锛堝缓璁<U+E1BB>娇鐢<U+3128><U+E179>鍗曞彿锟?`
  L43 (1): `* @param requestId 骞傜瓑鎬<U+0446><U+E1EC>锟絀D`

### `scm-inventory/api/src/main/java/com/scmcloud/inventory/api/InventoryTccService.java` - 9 mojibake chars

  L15 (1): `*   <li>Try: 棰勭暀搴撳瓨锛堝皢鍙<U+E21C>敤搴撳瓨杞<U+E0FF>负閿佸畾搴撳瓨锟?li>`
  L17 (1): `*   <li>Cancel: 鍙栨秷棰勭暀锛堥噴鏀鹃攣瀹氬簱瀛樹负鍙<U+E21C>敤搴撳瓨锟?li>`
  L27 (1): `* Try 闃舵<U+E18C>锛氶<U+E569>鐣欏簱锟?`
  L33 (1): `* @param businessKey 涓氬姟閿<U+E1C6>紙璁<U+3220>崟鍙凤級锛岀敤浜庡箓绛夋<U+20AC><U+0444>帶锟?`
  L48 (1): `* Confirm 闃舵<U+E18C>锛氱<U+2018>璁<U+3089><U+E569>锟?`
  ... 4 more lines

### `scm-inventory/service/src/main/java/com/scmcloud/inventory/InventoryServiceApplication.java` - 12 mojibake chars

  L12 (1): `* 搴撳瓨鏈嶅姟鍚<U+E21A>姩锟?`
  L14 (1): `* <p>搴撳瓨鏈嶅姟璐負矗锟?`
  L16 (1): `*     <li>搴撳瓨绠<U+FF04>悊锟堟煡璇<U+20AC>佽皟鏁淬<U+20AC>佽浆绉伙級</li>`
  L17 (1): `*     <li>搴撳瓨棰勫崰涓庨噴鏀撅紙璁<U+3220>崟鍦烘櫙锟?/li>`
  L21 (2): `*     <li>Redis 缂撳瓨锟絃ua 鑴氭湰闃茶秴锟?/li>`
  ... 6 more lines

### `scm-inventory/service/src/main/java/com/scmcloud/inventory/dto/InventoryAdjustRequest.java` - 3 mojibake chars

  L44 (1): `* 鍏宠仈涓氬姟鍗曞彿锛堥噰璐<U+E15E>崟鍙枫<U+20AC>侀攢鍞<U+E1BC>崟鍙风瓑锟?`
  L54 (1): `* 鎿嶄綔锟絀D`
  L59 (1): `* 鎿嶄綔浜哄<U+E758>锟?`

### `scm-inventory/service/src/main/java/com/scmcloud/inventory/dto/InventoryQueryRequest.java` - 8 mojibake chars

  L12 (1): `* <p>鏀<U+E21B>寔澶氱<U+E752>鏌<U+30E8><U+E1D7>鏉<U+2032>欢缁勫悎锟?`
  L17 (1): `*   <li>搴撳瓨鐘舵<U+20AC>佽繃婊<U+308F>紙缂鸿揣/浣庡簱锟芥<U+E11C>甯革拷/li>`
  L43 (2): `* 搴撳瓨鐘舵<U+20AC>佽繃婊<U+308F>紙NORMAL-姝<U+FF45>父, LOW_STOCK-浣庡簱锟?OUT_OF_STOCK-缂鸿揣锟?`
  L48 (1): `* 鏈<U+20AC>灏忓彲鐢<U+3125>簱瀛橈紙澶<U+0442>簬绛変簬锟?`
  L53 (1): `* 鏈<U+20AC>澶<U+0443>彲鐢<U+3125>簱瀛橈紙灏忎簬绛変簬锟?`
  ... 2 more lines

### `scm-inventory/service/src/main/java/com/scmcloud/inventory/dto/InventoryResponse.java` - 7 mojibake chars

  L33 (1): `* 鎬诲簱锟?`
  L43 (1): `* 閿佸畾搴撳瓨锛堝凡棰勫崰锟?`
  L58 (1): `* 鏈<U+20AC>澶<U+0443>簱锟?`
  L73 (2): `* 搴撳瓨鐘舵<U+20AC>侊紙NORMAL-姝<U+FF45>父, LOW_STOCK-浣庡簱锟?OUT_OF_STOCK-缂鸿揣锟?`
  L83 (1): `* 鏈<U+20AC>杩戝叆搴撴椂锟?`
  ... 1 more lines

### `scm-inventory/service/src/main/java/com/scmcloud/inventory/dto/InventoryStatsResponse.java` - 3 mojibake chars

  L29 (1): `* 鎬诲簱瀛樻暟锟?`
  L49 (1): `* 搴撳瓨鎬讳环锟?`
  L59 (1): `* 浣庡簱锟絊KU 鏁伴噺`

### `scm-inventory/service/src/main/java/com/scmcloud/inventory/dto/InventoryTransferRequest.java` - 2 mojibake chars

  L51 (1): `* 鎿嶄綔锟絀D`
  L56 (1): `* 鎿嶄綔浜哄<U+E758>锟?`


## scm-logistics (5 files)

### `scm-logistics/api/src/main/java/com/scmcloud/logistics/api/LogisticsDubboService.java` - 3 mojibake chars

  L9 (1): `* <p>鎻愪緵杩愬崟鍒涘缓銆佹煡璇<U+E76C><U+20AC>佺墿娴佺姸鎬佹洿鏂扮瓑鏍稿績鍔熻兘锛屼緵鍏朵粬寰<U+E1BD>湇鍔<U+FFE0><U+20AC>氳繃 RPC 璋冪敤锟?`
  L28 (1): `* @return 杩愬崟淇<U+2103>伅锛屼笉瀛樺湪鏃惰繑锟絥ull`
  L36 (1): `* @param status 鏂扮姸锟?`

### `scm-logistics/service/src/main/java/com/scmcloud/logistics/domain/entity/TmsTracking.java` - 1 mojibake chars

  L19 (1): `* 鐗<U+2542>祦杞<U+3128>抗锟?`

### `scm-logistics/service/src/main/java/com/scmcloud/logistics/domain/entity/TmsWaybill.java` - 1 mojibake chars

  L19 (1): `* 杩愬崟锟?`

### `scm-logistics/service/src/main/java/com/scmcloud/logistics/mapper/TmsTrackingMapper.java` - 1 mojibake chars

  L8 (1): `* 鐗<U+2542>祦杞<U+3128>抗锟組apper 鎺<U+30E5>彛`

### `scm-logistics/service/src/main/java/com/scmcloud/logistics/mapper/TmsWaybillMapper.java` - 1 mojibake chars

  L8 (1): `* 杩愬崟锟組apper 鎺<U+30E5>彛`


## scm-notify (10 files)

### `scm-notify/api/src/main/java/com/scmcloud/notify/api/NotifyDubboService.java` - 3 mojibake chars

  L11 (1): `* <p>鎻愪緵鍗曟潯/鎵归噺閫氱煡鍙戦<U+20AC>佺瓑鏍稿績鍔熻兘锛屼緵鍏朵粬寰<U+E1BD>湇鍔<U+FFE0><U+20AC>氳繃 RPC 璋冪敤锟?`
  L22 (1): `* @return 鍙戦<U+20AC>佺粨锟?`
  L30 (1): `* @return 鍙戦<U+20AC>佺粨锟?`

### `scm-notify/api/src/main/java/com/scmcloud/notify/api/dto/BatchNotifyResult.java` - 1 mojibake chars

  L7 (1): `* 鎵归噺閫氱煡鍙戦<U+20AC>佺粨锟?`

### `scm-notify/api/src/main/java/com/scmcloud/notify/api/dto/NotifyResult.java` - 1 mojibake chars

  L7 (1): `* 閫氱煡鍙戦<U+20AC>佺粨锟?`

### `scm-notify/service/src/main/java/com/scmcloud/notify/domain/entity/SysNotificationTemplate.java` - 1 mojibake chars

  L15 (1): `* 閫氱煡妯<U+2103>澘锟?`

### `scm-notify/service/src/main/java/com/scmcloud/notify/domain/entity/SysUserNotificationPreference.java` - 1 mojibake chars

  L15 (1): `* 鐢<U+3126>埛閫氱煡鍋忓<U+30BD>锟?`

### `scm-notify/service/src/main/java/com/scmcloud/notify/mapper/SysNotificationTemplateMapper.java` - 1 mojibake chars

  L8 (1): `* 閫氱煡妯<U+2103>澘锟組apper 鎺<U+30E5>彛`

### `scm-notify/service/src/main/java/com/scmcloud/notify/mapper/SysUserNotificationPreferenceMapper.java` - 1 mojibake chars

  L8 (1): `* 鐢<U+3126>埛閫氱煡鍋忓<U+30BD>锟組apper 鎺<U+30E5>彛`

### `scm-notify/service/src/main/java/com/scmcloud/notify/service/ISysNotificationAuditService.java` - 1 mojibake chars

  L11 (1): `* 閫氱煡鍙戦<U+20AC>佸<U+E178>璁<U+00A4><U+3003> 鏈嶅姟锟?`

### `scm-notify/service/src/main/java/com/scmcloud/notify/service/ISysNotificationTemplateService.java` - 1 mojibake chars

  L11 (1): `* 閫氱煡妯<U+2103>澘锟芥湇鍔<U+2605>拷`

### `scm-notify/service/src/main/java/com/scmcloud/notify/service/ISysUserNotificationPreferenceService.java` - 1 mojibake chars

  L11 (1): `* 鐢<U+3126>埛閫氱煡鍋忓<U+30BD>锟芥湇鍔<U+2605>拷`


## scm-order (14 files)

### `scm-order/api/src/main/java/com/scmcloud/order/api/OrderDubboService.java` - 2 mojibake chars

  L25 (1): `* @param orderNo 璁<U+3220>崟锟?`
  L33 (1): `* @param orderNo 璁<U+3220>崟锟?`

### `scm-order/service/src/main/java/com/scmcloud/order/config/XxlJobConfig.java` - 2 mojibake chars

  L13 (1): `* <p>閰嶇疆 XXL-Job 鎵<U+0446><U+E511>鍣<U+E7D2>紝鑷<U+E044>姩娉<U+3125>唽锟絏XL-Job Admin`
  L47 (1): `* XXL-Job 鎵<U+0446><U+E511>锟?`

### `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdOrderItem.java` - 1 mojibake chars

  L19 (1): `* 璁<U+3220>崟鏄庣粏锟?`

### `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdPayment.java` - 1 mojibake chars

  L18 (1): `* 鏀<U+E219>粯璁板綍锟?`

### `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdRefund.java` - 1 mojibake chars

  L19 (1): `* 閫<U+20AC>锟介<U+20AC><U+20AC>璐<U+0446><U+3003>`

### `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdStatusHistory.java` - 1 mojibake chars

  L17 (1): `* 璁<U+3220>崟鐘舵佹祦杞<U+E100>巻锟?`

### `scm-order/service/src/main/java/com/scmcloud/order/mapper/OrdOrderItemMapper.java` - 1 mojibake chars

  L9 (1): `* 璁<U+3220>崟鏄庣粏锟組apper 鎺<U+30E5>彛`

### `scm-order/service/src/main/java/com/scmcloud/order/mapper/OrdPaymentMapper.java` - 1 mojibake chars

  L9 (1): `* 鏀<U+E219>粯璁板綍锟組apper 鎺<U+30E5>彛`

### `scm-order/service/src/main/java/com/scmcloud/order/mapper/OrdRefundMapper.java` - 1 mojibake chars

  L9 (1): `* 閫<U+20AC>锟介<U+20AC><U+20AC>璐<U+0446><U+3003> Mapper 鎺<U+30E5>彛`

### `scm-order/service/src/main/java/com/scmcloud/order/mapper/OrdStatusHistoryMapper.java` - 1 mojibake chars

  L9 (1): `* 璁<U+3220>崟鐘舵<U+20AC>佹祦杞<U+E100>巻锟組apper 鎺<U+30E5>彛`

### `scm-order/service/src/main/java/com/scmcloud/order/service/IOrdOrderItemService.java` - 1 mojibake chars

  L11 (1): `* 璁<U+3220>崟鏄庣粏锟芥湇鍔<U+2605>拷`

### `scm-order/service/src/main/java/com/scmcloud/order/service/IOrdPaymentService.java` - 1 mojibake chars

  L9 (1): `* 鏀<U+E219>粯璁板綍锟芥湇鍔<U+2605>拷`

### `scm-order/service/src/main/java/com/scmcloud/order/service/IOrdRefundService.java` - 2 mojibake chars

  L11 (2): `* 閫<U+20AC>锟介<U+20AC><U+20AC>璐<U+0446><U+3003> 鏈嶅姟锟?`

### `scm-order/service/src/main/java/com/scmcloud/order/service/IOrdStatusHistoryService.java` - 1 mojibake chars

  L11 (1): `* 璁<U+3220>崟鐘舵<U+20AC>佹祦杞<U+E100>巻锟芥湇鍔<U+2605>拷`


## scm-product (13 files)

### `scm-product/api/src/main/java/com/scmcloud/product/api/ProductDubboService.java` - 4 mojibake chars

  L11 (1): `* <p>鎻愪緵鍟嗗搧銆丼KU 鏌<U+30E8><U+E1D7>绛夋牳蹇冨姛鑳斤紝渚涘叾浠栧井鏈嶅姟閫氳繃 RPC 璋冪敤锟?`
  L22 (1): `* @return 鍟嗗搧淇<U+2103>伅锛屼笉瀛樺湪鏃惰繑锟絥ull`
  L30 (1): `* @return SKU 淇<U+2103>伅锛屼笉瀛樺湪鏃惰繑锟絥ull`
  L45 (1): `* @param keyword 鎼滅储鍏抽敭锟?`

### `scm-product/service/src/main/java/com/scmcloud/product/config/KafkaConfig.java` - 7 mojibake chars

  L18 (1): `* Kafka 閰嶇疆锟?`
  L54 (1): `// 鑷<U+E044>姩鎻愪氦鍋忕<U+0429>閲忥紙璁剧疆锟絝alse锛屾墜鍔<U+3126>彁浜<U+308F>級`
  L60 (1): `// Key 鍙嶅簭鍒楀寲锟?`
  L63 (1): `// Value 鍙嶅簭鍒楀寲锟?`
  L69 (1): `// 浼氳瘽瓒呮椂鏃堕棿锟? 绉掞級`
  ... 2 more lines

### `scm-product/service/src/main/java/com/scmcloud/product/domain/entity/ProdBrand.java` - 1 mojibake chars

  L16 (1): `* 鍟嗗搧鍝佺墝锟?`

### `scm-product/service/src/main/java/com/scmcloud/product/domain/entity/ProdCategory.java` - 1 mojibake chars

  L16 (1): `* 鍟嗗搧鍒嗙被锟?`

### `scm-product/service/src/main/java/com/scmcloud/product/domain/entity/ProdSku.java` - 1 mojibake chars

  L17 (1): `* SKU 搴撳瓨鍗曚綅锟?`

### `scm-product/service/src/main/java/com/scmcloud/product/domain/entity/ProdSpu.java` - 1 mojibake chars

  L17 (1): `* SPU 鏍囧噯浜<U+0443>搧鍗曞厓锟?`

### `scm-product/service/src/main/java/com/scmcloud/product/mapper/ProdBrandMapper.java` - 1 mojibake chars

  L9 (1): `* 鍟嗗搧鍝佺墝锟組apper 鎺<U+30E5>彛`

### `scm-product/service/src/main/java/com/scmcloud/product/mapper/ProdCategoryMapper.java` - 1 mojibake chars

  L9 (1): `* 鍟嗗搧鍒嗙被锟組apper 鎺<U+30E5>彛`

### `scm-product/service/src/main/java/com/scmcloud/product/mapper/ProdSkuMapper.java` - 1 mojibake chars

  L9 (1): `* SKU搴撳瓨鍗曚綅锟組apper 鎺<U+30E5>彛`

### `scm-product/service/src/main/java/com/scmcloud/product/mapper/ProdSpuMapper.java` - 1 mojibake chars

  L9 (1): `* SPU鏍囧噯浜<U+0443>搧鍗曞厓锟組apper 鎺<U+30E5>彛`

### `scm-product/service/src/main/java/com/scmcloud/product/search/document/ProductDocument.java` - 13 mojibake chars

  L16 (1): `* <p>鐢<U+3124>簬 Elasticsearch 鍏<U+3126>枃鎼滅储鐨勫晢鍝佹枃锟?`
  L18 (1): `* <p>绱<U+3220>紩璁捐<U+E178>锟?`
  L19 (1): `* - 浣跨敤 IK 鍒嗚瘝鍣<U+3128>繘琛屼腑鏂囧垎锟?`
  L22 (2): `* - 5 鍒嗙墖锟?鍓<U+E21B>湰锛屾敮鎸佹按骞虫墿锟?`
  L45 (2): `* SPU 鍚嶇<U+041E>锛堜娇锟絀K 鍒嗚瘝鍣<U+E7D2>紝鏀<U+E21B>寔鍏<U+3126>枃鎼滅储锟?`
  ... 6 more lines

### `scm-product/service/src/main/java/com/scmcloud/product/search/dto/ProductSearchRequest.java` - 1 mojibake chars

  L10 (1): `* <p>鏀<U+E21B>寔澶氭潯浠剁粍鍚堟悳锟?`

### `scm-product/service/src/main/java/com/scmcloud/product/search/repository/ProductSearchRepository.java` - 21 mojibake chars

  L15 (1): `* <p>鍩轰簬 Spring Data Elasticsearch 鐨勫晢鍝佹悳绱<U+3221>帴锟?`
  L17 (1): `* <p>鏀<U+E21B>寔鍔熻兘锟?`
  L18 (1): `* - 鍏<U+3126>枃鎼滅储锛坰puName, description, seoKeywords锟?`
  L32 (1): `* 锟絊PU 鍚嶇<U+041E>鎼滅储锛堜笂鏋跺晢鍝侊級`
  L35 (1): `* @param status   鍟嗗搧鐘舵<U+20AC>侊紙1-涓婃灦锟?`
  ... 16 more lines


## scm-purchase (11 files)

### `scm-purchase/api/src/main/java/com/scmcloud/purchase/api/PurchaseDubboService.java` - 5 mojibake chars

  L9 (1): `* <p>鎻愪緵閲囪喘鐢宠<U+E1EC>銆侀噰璐<U+E15E>崟鏌<U+30E8><U+E1D7>銆佹敹璐<U+0445><U+2018>璁<U+3087>瓑鏍稿績鍔熻兘锛屼緵鍏朵粬寰<U+E1BD>湇鍔<U+FFE0><U+20AC>氳繃 RPC 璋冪敤锟?`
  L20 (1): `* @return 閲囪喘鍗曚俊锟?`
  L25 (1): `* 鏍规嵁 ID 鏌<U+30E8><U+E1D7>閲囪喘锟?`
  L27 (1): `* @param id 閲囪喘锟絀D`
  L35 (1): `* @param receiptId 鏀惰揣锟絀D`

### `scm-purchase/service/src/main/java/com/scmcloud/purchase/domain/entity/PurContract.java` - 1 mojibake chars

  L20 (1): `* 閲囪喘鍚堝悓锟?`

### `scm-purchase/service/src/main/java/com/scmcloud/purchase/domain/entity/PurOrderItem.java` - 1 mojibake chars

  L18 (1): `* 閲囪喘璁<U+3220>崟鏄庣粏锟?`

### `scm-purchase/service/src/main/java/com/scmcloud/purchase/domain/entity/PurPlan.java` - 1 mojibake chars

  L18 (1): `* 閲囪喘璁<U+2033>垝琛<U+E7D2>紙MRP锟?`

### `scm-purchase/service/src/main/java/com/scmcloud/purchase/domain/entity/PurPlanItem.java` - 1 mojibake chars

  L18 (1): `* 閲囪喘璁<U+2033>垝鏄庣粏锟?`

### `scm-purchase/service/src/main/java/com/scmcloud/purchase/domain/entity/PurPriceComparison.java` - 1 mojibake chars

  L16 (1): `* 姣斾环鍒嗘瀽锟?`

### `scm-purchase/service/src/main/java/com/scmcloud/purchase/domain/entity/PurPriceComparisonItem.java` - 1 mojibake chars

  L17 (1): `* 姣斾环鏄庣粏锟?`

### `scm-purchase/service/src/main/java/com/scmcloud/purchase/domain/entity/PurQuotation.java` - 1 mojibake chars

  L18 (1): `* 渚涘簲鍟嗘姤浠峰崟锟?`

### `scm-purchase/service/src/main/java/com/scmcloud/purchase/domain/entity/PurReceiptItem.java` - 1 mojibake chars

  L18 (1): `* 閲囪喘鍏<U+30E5>簱鏄庣粏锟?`

### `scm-purchase/service/src/main/java/com/scmcloud/purchase/domain/entity/PurRequestItem.java` - 1 mojibake chars

  L17 (1): `* 閲囪喘鐢宠<U+E1EC>鏄庣粏锟?`

### `scm-purchase/service/src/main/java/com/scmcloud/purchase/mapper/PurOrderItemMapper.java` - 1 mojibake chars

  L8 (1): `* 閲囪喘璁<U+3220>崟鏄庣粏锟組apper 鎺<U+30E5>彛`


## scm-system (51 files)

### `scm-system/api/src/main/java/com/scmcloud/system/api/UserDubboService.java` - 8 mojibake chars

  L12 (2): `* Dubbo API 鐢<U+3124>簬鏈嶅姟闂寸殑楂橀<U+20AC>熷唴锟絉PC 閫氫俊锟?`
  L19 (3): `* 锟絊pring Security 锟経serDetailsService 浣跨敤锟?`
  L21 (1): `* @param username 瑕佹悳绱<U+3222>殑鐢<U+3126>埛锟?`
  L22 (2): `* @return 鍖呭惈韬<U+E0A1>唤楠岃瘉淇<U+2103>伅锟絊ecurityUser 瀵硅薄锛屽<U+E6E7>鏋滄壘涓嶅埌鍒欒繑锟絥ull`

### `scm-system/service/src/main/java/com/scmcloud/system/SysServiceApplication.java` - 1 mojibake chars

  L9 (1): `* Service 妯<U+2033>潡鍚<U+E21A>姩锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/controller/SysPermissionController.java` - 8 mojibake chars

  L18 (1): `* 鏉冮檺绠<U+FF04>悊鎺<U+0443>埗锟?`
  L31 (1): `* 鏌<U+30E8><U+E1D7>鏉冮檺锟?`
  L103 (2): `* 鏌<U+30E8><U+E1D7>鐢<U+3126>埛鏉冮檺锛堢敤锟紽eign 璋冪敤锟?`
  L114 (3): `* 鏍规嵁 URL 锟紿TTP 鏂规硶鏌<U+30E8><U+E1D7>鏉冮檺锛堢敤锟紽eign 璋冪敤锟?`
  L124 (1): `* 鏌<U+30E8><U+E1D7>鎵<U+20AC>锟紸PI 鏉冮檺锛堢敤浜庡姩鎬佹潈闄愬姞杞斤級`

### `scm-system/service/src/main/java/com/scmcloud/system/controller/SysRoleController.java` - 2 mojibake chars

  L18 (1): `* 瑙掕壊绠<U+FF04>悊鎺<U+0443>埗锟?`
  L44 (1): `* 鏌<U+30E8><U+E1D7>鎵<U+20AC>鏈夎<U+E757>鑹诧紙鐢<U+3124>簬涓嬫媺閫夋嫨锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/domain/entity/NotificationAuditLog.java` - 1 mojibake chars

  L109 (1): `* 鍙戦<U+20AC>佺姸鎬佹灇锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/domain/entity/SysDataPermissionRule.java` - 1 mojibake chars

  L16 (1): `* 鏁版嵁鏉冮檺瑙勫垯锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/domain/entity/SysDept.java` - 2 mojibake chars

  L14 (1): `* 閮<U+3129>棬锟?`
  L84 (1): `// ==================== 鍐椾綑瀛楁<U+E18C>锛堟潵鑷猟b_user.sys_user锟?=================`

### `scm-system/service/src/main/java/com/scmcloud/system/domain/entity/SysPermission.java` - 1 mojibake chars

  L14 (1): `* 鏉冮檺锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/domain/entity/SysRole.java` - 1 mojibake chars

  L19 (1): `* 瑙掕壊锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/domain/entity/SysRoleDataRule.java` - 1 mojibake chars

  L12 (1): `* 瑙掕壊鏁版嵁鏉冮檺瑙勫垯鍏宠仈锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/domain/entity/SysRoleDept.java` - 2 mojibake chars

  L12 (2): `* 瑙掕壊閮<U+3129>棬鍏宠仈锟?鐢<U+3124>簬鑷<U+E044>畾涔夋暟鎹<U+E1BD>潈闄愯寖锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/domain/entity/SysRolePermission.java` - 1 mojibake chars

  L12 (1): `* 瑙掕壊鏉冮檺鍏宠仈锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/domain/entity/SysTempPermission.java` - 2 mojibake chars

  L12 (1): `* 涓存椂鏉冮檺锟?鐢<U+3124>簬涓存椂鎺堟潈`
  L51 (1): `* 鍒<U+3086>柇鏄<U+E21A>惁鍦<U+3126>湁鏁堟湡锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/domain/entity/SysUserOauth.java` - 1 mojibake chars

  L65 (1): `* OAuth 鎻愪緵鍟嗘灇锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/domain/entity/SysUserRole.java` - 6 mojibake chars

  L13 (1): `* 鐢<U+3126>埛瑙掕壊鍏宠仈锟? 鏀<U+E21B>寔涓存椂瑙掕壊鎺堟潈`
  L52 (2): `// ==================== 鍐椾綑瀛楁<U+E18C>锛堟潵锟絛b_user.sys_user锟?==================`
  L64 (1): `* 瀹<U+2103>壒鐘舵<U+20AC>佹灇锟?`
  L83 (1): `* 鍒<U+3086>柇鏄<U+E21A>惁涓轰复鏃舵巿锟?`
  L90 (1): `* 鍒<U+3086>柇鏄<U+E21A>惁鍦<U+3126>湁鏁堟湡锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/evaluator/CustomPermissionEvaluator.java` - 5 mojibake chars

  L28 (1): `* 鍒<U+3086>柇鐢<U+3126>埛鏄<U+E21A>惁鏈夋寚瀹氭潈锟?`
  L39 (1): `// 妫<U+20AC>鏌<U+30E7>敤鎴锋槸鍚<U+FE3D>湁璇<U+30E6>潈锟?`
  L49 (1): `* 鍒<U+3086>柇鐢<U+3126>埛鏄<U+E21A>惁鏈夋寚瀹氳祫婧愮殑鏉冮檺锛堝熀浜庤祫婧怚D锟?`
  L58 (1): `// 鍙<U+E219>互瀹炵幇鏇村<U+E632>鏉傜殑璧勬簮绾<U+0444>潈闄愭帶锟?`
  L59 (1): `// 渚嬪<U+E6E7>锛氭<U+E5C5>鏌<U+30E7>敤鎴锋槸鍚<U+FE40>彲浠<U+30E8><U+E196>闂<U+E1BE>壒瀹欼D鐨勮祫锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/event/DataSyncEventPublisher.java` - 3 mojibake chars

  L18 (1): `* 鏁版嵁鍚屾<U+E11E>浜嬩欢鍙戝竷锟?`
  L20 (1): `* 灏佽<U+E5CA>涓氬姟瀹炰綋锟紻ataSyncEvent 鐨勮浆鎹<U+E76E>紝`
  L21 (1): `* 濮旀墭缁欏叏灞<U+20AC> DataSyncPublisher 鍙戝竷锟終afka`

### `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysDataPermissionRuleMapper.java` - 4 mojibake chars

  L31 (1): `* 鏍规嵁璧勬簮绫诲瀷鏌<U+30E8><U+E1D7>鍚<U+E21C>敤鐨勮<U+E749>锟?`
  L41 (1): `* 鏍规嵁瑙掕壊 ID鏌<U+30E8><U+E1D7>鍏宠仈鐨勮<U+E749>锟?`
  L52 (1): `* 鏍规嵁鐢<U+3126>埛ID鏌<U+30E8><U+E1D7>鍏宠仈鐨勮<U+E749>鍒欙紙閫氳繃鐢<U+3126>埛瑙掕壊锟?`
  L68 (1): `* 妫<U+20AC>鏌<U+30E8><U+E749>鍒欑紪鐮佹槸鍚<U+FE40>瓨锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysDeptMapper.java` - 14 mojibake chars

  L13 (1): `* 閮<U+3129>棬锟組apper 鎺<U+30E5>彛`
  L15 (3): `* 娉<U+3126>剰锛氭<U+E11D> Mapper 鍙<U+E044><U+E629>锟絛b_org 搴撲腑锟絪ys_dept 锟?`
  L16 (1): `* 闇<U+20AC>瑕佽幏鍙栭儴闂<U+3128>礋璐<U+FF44>汉淇<U+2103>伅鏃讹紝璇峰湪 Service 灞傝仛鍚堟煡锟?`
  L26 (1): `* 鏌<U+30E8><U+E1D7>鎵<U+20AC>鏈夐儴闂<U+3125>垪琛<U+E7D2>紙涓嶅寘鍚<U+E0A5>礋璐<U+FF44>汉淇<U+2103>伅锟?`
  L86 (1): `* 鐢<U+3124>簬浼樺寲 getDeptTree 绛夐渶瑕佺粺璁<U+2033><U+E63F>涓<U+E048>儴闂<U+3125>瓙閮<U+3129>棬鏁扮殑鍦烘櫙锛岄伩锟絅+1 鏌<U+30E8><U+E1D7>`
  ... 7 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysPermissionMapper.java` - 6 mojibake chars

  L17 (1): `* 鏉冮檺锟組apper 鎺<U+30E5>彛`
  L55 (1): `* 鏌<U+30E8><U+E1D7>瑙掕壊鏉冮檺锟?`
  L74 (1): `* 妫<U+20AC>鏌<U+30E8>祫婧愭潈锟?`
  L92 (1): `* 鏌<U+30E8><U+E1D7>鐢<U+3126>埛鑿滃崟锟?`
  L114 (1): `* 鏌<U+30E8><U+E1D7>瀛愭潈锟?`
  ... 1 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysRoleDataRuleMapper.java` - 3 mojibake chars

  L31 (1): `* 鍒犻櫎瑙掕壊鐨勬墍鏈夎<U+E749>鍒欏叧锟?`
  L40 (1): `* 鍒犻櫎瑙勫垯鐨勬墍鏈夎<U+E757>鑹插叧锟?`
  L75 (1): `* 鐢<U+3124>簬鍒犻櫎瑙掕壊鏃舵竻鐞嗘暟鎹<U+E1BD>潈闄愯<U+E749>鍒欏叧锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysRoleDeptMapper.java` - 9 mojibake chars

  L31 (1): `* 鏍规嵁瑙掕壊ID鏌<U+30E8><U+E1D7>鍏宠仈淇<U+2103>伅锛堝寘鍚<U+E0A2>瓙閮<U+3129>棬鏍囪<U+E187>锟?`
  L39 (2): `// 娉<U+3126>剰锛歠indAccessibleDeptIds 鏂规硶宸茬<U+0429>锟絊ervice 灞傚疄锟?`
  L41 (2): `// 1. 鍏堥<U+20AC>氳繃 findByRoleId 鏌<U+30E8><U+E1D7>瑙掕壊閮<U+3129>棬鍏宠仈锛堝寘锟絠nclude_children 鏍囪<U+E187>锟?`
  L43 (1): `// 3. 鍚堝苟鎵<U+20AC>鏈夐儴锟絀D`
  L64 (1): `* 鍒犻櫎瑙掕壊鐨勬墍鏈夐儴闂<U+3125>叧锟?`
  ... 2 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysRoleMapper.java` - 6 mojibake chars

  L14 (1): `* 瑙掕壊锟組apper 鎺<U+30E5>彛`
  L25 (1): `* 妫<U+20AC>鏌<U+30E8><U+E757>鑹茬紪鐮佹槸鍚<U+FE40>瓨鍦<U+E7D2>紙涓嶈<U+20AC>冭檻绉熸埛锟?`
  L34 (1): `* 妫<U+20AC>鏌<U+30E8><U+E757>鑹茬紪鐮佸湪鎸囧畾绉熸埛涓嬫槸鍚<U+FE40>瓨锟?`
  L35 (1): `* 鐢<U+3124>簬澶氱<U+E764>鎴风幆澧冧笅鐨勫敮涓<U+20AC>鎬<U+0444>牎锟?`
  L80 (1): `* 鐢<U+3124>簬楠岃瘉瑙掕壊褰掑睘锛圢ULL 琛<U+3127><U+305A>骞冲彴瑙掕壊锟?`
  ... 1 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysRolePermissionMapper.java` - 5 mojibake chars

  L12 (1): `* 瑙掕壊鏉冮檺鍏宠仈锟組apper 鎺<U+30E5>彛`
  L40 (1): `* 鍒犻櫎瑙掕壊鐨勬墍鏈夋潈闄愬叧锟?`
  L49 (1): `* 鍒犻櫎鏉冮檺鐨勬墍鏈夎<U+E757>鑹插叧锟?`
  L58 (1): `* 妫<U+20AC>鏌<U+30E8><U+E757>鑹叉槸鍚<U+FE3D>嫢鏈夋寚瀹氭潈锟?`
  L92 (1): `* 缁熻<U+E178>浣跨敤璇<U+30E6>潈闄愮殑瑙掕壊锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysTempPermissionMapper.java` - 6 mojibake chars

  L15 (1): `* 涓存椂鏉冮檺锟組apper 鎺<U+30E5>彛`
  L25 (1): `* 鏌<U+30E8><U+E1D7>鐢<U+3126>埛鏈夋晥鐨勪复鏃舵潈锟?`
  L37 (1): `* 鏌<U+30E8><U+E1D7>鐢<U+3126>埛鏈夋晥鐨勪复鏃舵潈锟絀D 鍒楄<U+3003>`
  L49 (1): `* 鏌<U+30E8><U+E1D7>鍗冲皢杩囨湡鐨勪复鏃舵潈闄愶紙鐢<U+3124>簬娓呯悊浠诲姟锟?`
  L59 (1): `* 绂佺敤杩囨湡鐨勪复鏃舵潈锟?`
  ... 1 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysUserMapper.java` - 11 mojibake chars

  L14 (1): `* 鐢<U+3126>埛锟組apper 鎺<U+30E5>彛`
  L16 (3): `* 娉<U+3126>剰锛氭<U+E11D> Mapper 鍙<U+E044><U+E629>锟絛b_user 搴撲腑锟絪ys_user 锟?`
  L17 (2): `* 娑夊強瑙掕壊銆佹潈闄愮殑鏌<U+30E8><U+E1D7>璇蜂娇鐢<U+3125><U+E1EE>搴旂殑 Mapper 锟絊ervice 灞傝仛锟?`
  L27 (1): `* 鏍规嵁鐢<U+3126>埛鍚嶆煡璇<U+3222>敤锟?`
  L45 (1): `* 鏇存柊鏈<U+20AC>鍚庣櫥褰曚俊锟?`
  ... 3 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysUserOauthMapper.java` - 3 mojibake chars

  L12 (1): `* OAuth绗<U+E0FF>笁鏂圭櫥褰曠粦锟組apper 鎺<U+30E5>彛`
  L49 (1): `* 妫<U+20AC>鏌<U+30E7>敤鎴锋槸鍚<U+FE40>凡缁戝畾鎸囧畾鎻愪緵锟?`
  L58 (1): `* 鏇存柊鏈<U+20AC>鍚庣櫥褰曟椂锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysUserRoleMapper.java` - 27 mojibake chars

  L18 (1): `* 澶勭悊 db_permission 搴撲腑锟絪ys_user_role銆乻ys_role銆乻ys_permission 绛夎<U+3003>`
  L28 (1): `* 鏌<U+30E8><U+E1D7>鐢<U+3126>埛鐨勬湁鏁堣<U+E757>锟絀D 鍒楄<U+3003>`
  L39 (1): `* 鏌<U+30E8><U+E1D7>鐢<U+3126>埛鐨勬湁鏁堣<U+E757>鑹茬紪鐮佸垪锟?`
  L75 (1): `* 鏌<U+30E8><U+E1D7>鎷<U+30E6>湁鎸囧畾瑙掕壊鐨勭敤锟絀D 鍒楄<U+3003>`
  L98 (1): `* 鏌<U+30E8><U+E1D7>鐢<U+3126>埛鐨勬湁鏁堟潈闄愮紪鐮佸垪锟?`
  ... 21 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/notification/NotificationService.java` - 3 mojibake chars

  L23 (1): `* @param username     鐢<U+3126>埛鍚嶏紙鐢<U+3124>簬绔欏唴娑堟伅锟?`
  L24 (1): `* @param email        閭<U+E1BE><U+E188>锛堢敤浜庨偖浠堕<U+20AC>氱煡锟?`
  L52 (1): `* 鍙戦<U+20AC>侀<U+20AC>氱煡鍒版寚瀹氭笭锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/service/ISysDeptService.java` - 11 mojibake chars

  L12 (1): `* 閮<U+3129>棬锟芥湇鍔<U+2605>拷`
  L21 (1): `* 鏌<U+30E8><U+E1D7>閮<U+3129>棬鏍戯紙鏍戝舰缁撴瀯锛屼粠鏍硅妭鐐瑰紑濮嬶級锟?`
  L23 (1): `* @return 閮<U+3129>棬鏍戝垪锟?`
  L28 (1): `* 鏌<U+30E8><U+E1D7>鎸囧畾閮<U+3129>棬鍙婂叾鎵<U+20AC>鏈夊瓙閮<U+3129>棬ID锛堝寘鍚<U+E0A5>嚜韬<U+E0AC>級锟?`
  L31 (1): `* @return 閮<U+3129>棬ID鍒楄<U+3003>锛堝寘鍚<U+E0A5>嚜韬<U+E0A2>強鎵<U+20AC>鏈夊瓙閮<U+3129>棬锟?`
  ... 6 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/service/ISysPermissionService.java` - 15 mojibake chars

  L15 (1): `* 鏉冮檺锟芥湇鍔<U+2605>拷`
  L24 (1): `* 鍒<U+3086>柇鐢<U+3126>埛鏄<U+E21A>惁鎷<U+30E6>湁鎸囧畾鏉冮檺缂栫爜锟?`
  L28 (1): `* @return true 琛<U+3127><U+305A>鎷<U+30E6>湁璇<U+30E6>潈闄愶紱false 琛<U+3127><U+305A>涓嶆嫢锟?`
  L39 (1): `* @return true 琛<U+3127><U+305A>鎷<U+30E6>湁璇<U+30E8>祫婧愭潈闄愶紱false 琛<U+3127><U+305A>涓嶆嫢锟?`
  L62 (1): `* @return 鏉冮檺鏍戝垪锟?`
  ... 10 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/service/ISysRoleService.java` - 10 mojibake chars

  L13 (1): `* 瑙掕壊锟芥湇鍔<U+2605>拷`
  L22 (1): `* 鍒嗛<U+3009>鏌<U+30E8><U+E1D7>瑙掕壊鍒楄<U+3003>锟?`
  L24 (1): `* @param pageNum  椤电爜锛屼粠 1 寮<U+20AC>锟?`
  L26 (1): `* @param roleName 瑙掕壊鍚嶇<U+041E>锛堝彲閫夛紝鏀<U+E21B>寔妯<U+FF04>硦鏌<U+30E8><U+E1D7>锟?`
  L32 (1): `* 鏌<U+30E8><U+E1D7>鎵<U+20AC>鏈夎<U+E757>鑹诧紙涓嶅垎椤碉級锟?`
  ... 5 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/service/Impl/UserDetailsServiceImpl.java` - 6 mojibake chars

  L39 (2): `// 1. 锟絛b_user 搴撴煡璇<U+3222>敤鎴峰熀鏈<U+E0FF>俊锟?`
  L46 (2): `// 2. 锟絛b_permission 搴撴煡璇<U+3222>敤鎴疯<U+E757>鑹诧紙璺<U+3125>簱鏌<U+30E8><U+E1D7>锟?`
  L49 (2): `// 3. 锟絛b_permission 搴撴煡璇<U+3222>敤鎴锋潈闄愶紙璺<U+3125>簱鏌<U+30E8><U+E1D7>锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/service/command/DeptRoleCrossDatabaseCommandService.java` - 4 mojibake chars

  L15 (1): `* 澶勭悊閮<U+3129>棬瑙掕壊鍏宠仈鐨勫啓鎿嶄綔锛坉b_permission锟?`
  L27 (1): `* 鍒犻櫎閮<U+3129>棬鐨勮<U+E757>鑹插叧锟?`
  L29 (1): `* 鐢<U+3124>簬鍒犻櫎閮<U+3129>棬鏃舵竻锟絛b_permission.sys_role_dept 涓<U+E160>殑鍏宠仈鏁版嵁`
  L30 (1): `* 璺<U+3125>簱鎿嶄綔锛歞b_org 锟絛b_permission`

### `scm-system/service/src/main/java/com/scmcloud/system/service/command/UserRoleCrossDatabaseCommandService.java` - 5 mojibake chars

  L17 (1): `* 澶勭悊鐢<U+3126>埛瑙掕壊鍏宠仈鐨勫啓鎿嶄綔锛坉b_permission锟?`
  L36 (1): `* @param createBy 鍒涘缓锟絀D`
  L59 (1): `* @param createBy      鍒涘缓锟絀D`
  L77 (1): `* 鍒犻櫎鐢<U+3126>埛鐨勬墍鏈夎<U+E757>鑹插叧锟?`
  L96 (1): `* 寤堕暱涓存椂瑙掕壊鏈夋晥锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/service/dubbo/UserDubboServiceImpl.java` - 2 mojibake chars

  L55 (2): `// 璋冪敤鐜版湁鏂规硶锛涘<U+E6E7>鏋滀笉闇<U+20AC>瑕侊紝鍒欏拷锟絣oginTime 鍙傛暟锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/service/dubbo/adapter/DubboPermissionServiceAdapter.java` - 11 mojibake chars

  L17 (2): `* 鍩轰簬 Dubbo 锟絇ermissionService 鎺<U+30E5>彛瀹炵幇锟?`
  L19 (3): `* <p>閲嶆瀯锛氬凡锟絚ommon/web 妯<U+2033>潡杩佺<U+0429>锟絪ystem/service 妯<U+2033>潡锟?`
  L20 (1): `* 杩欑<U+E0C1>鍚堟<U+E11C>纭<U+E1BE>殑鏋舵瀯鍒嗗眰鈥斺<U+20AC>斾笟鍔<U+2103><U+0101>鍧楁彁渚涘疄鐜帮紝`
  L24 (1): `* 浠<U+30E9>槻姝<U+3222>敱浜庢潈闄愭<U+E5C5>鏌<U+30E5><U+3051>璐<U+30E8><U+20AC>屽<U+E1F1>鑷存湭缁忔巿鏉冪殑璁块棶锟?`
  L26 (1): `* <p>鏋舵瀯浼樺娍锟?`
  ... 3 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/service/query/DeptCrossDatabaseQueryService.java` - 29 mojibake chars

  L21 (3): `* 澶勭悊涓庨儴闂<U+3127>浉鍏崇殑璺<U+3125>簱鏌<U+30E8><U+E1D7>鎿嶄綔锛坉b_org 锟絛b_user 锟絛b_permission锟?`
  L37 (1): `* 鏇夸唬锟絊ysDeptMapper.selectDeptTree`
  L39 (1): `* @return 閮<U+3129>棬 DTO 鍒楄<U+3003>锛堝寘鍚<U+E0A5>礋璐<U+FF44>汉濮撳悕锟?`
  L44 (2): `// 1. 锟給rg 搴撴煡璇<U+3221>墍鏈夐儴锟?`
  L56 (1): `// 3. 锟絬ser 搴撴壒閲忔煡璇<U+3223>礋璐<U+FF44>汉淇<U+2103>伅`
  ... 18 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/service/query/PermissionCrossDatabaseQueryService.java` - 3 mojibake chars

  L19 (2): `* 澶勭悊涓庢潈闄愮浉鍏崇殑璺<U+3125>簱鏌<U+30E8><U+E1D7>鎿嶄綔锛坉b_permission 锟絛b_user锟?`
  L31 (1): `* 鏌<U+30E8><U+E1D7>鐢<U+3126>埛鑿滃崟锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/service/query/RoleCrossDatabaseQueryService.java` - 26 mojibake chars

  L17 (3): `* 澶勭悊涓庤<U+E757>鑹茬浉鍏崇殑璺<U+3125>簱鏌<U+30E8><U+E1D7>鎿嶄綔锛坉b_permission 锟絛b_user 锟絛b_org锟?`
  L33 (1): `* 鑾峰彇瑙掕壊鐨勭瓑锟?`
  L35 (1): `* 鐢<U+3124>簬瑙掕壊鎺堟潈鏃剁殑鏉冮檺妫<U+20AC>锟?`
  L39 (1): `* @return 瑙掕壊绛夌骇锛坮ole_level锟?`
  L53 (1): `* 鐢<U+3124>簬瑙掕壊鎺堟潈鏃堕獙璇佽<U+E757>鑹插綊灞烇紙鍙<U+E047>兘鍒嗛厤鏈<U+E102><U+E764>鎴锋垨骞冲彴瑙掕壊锟?`
  ... 14 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/service/query/UserCrossDatabaseQueryService.java` - 26 mojibake chars

  L20 (3): `* 澶勭悊涓庣敤鎴风浉鍏崇殑璺<U+3125>簱鏌<U+30E8><U+E1D7>鎿嶄綔锛坉b_user 锟絛b_permission 锟絛b_org锟?`
  L69 (1): `* 鎵归噺鑾峰彇鐢<U+3126>埛鍩烘湰淇<U+2103>伅锛圡ap 褰<U+3220>紡锟?`
  L72 (1): `* @return 鐢<U+3126>埛 ID 锟界敤鎴峰疄浣?鏄犲皠`
  L82 (1): `* 鏌<U+30E8><U+E1D7>鐢<U+3126>埛瑙掕壊锛堝甫瑙掕壊鍚嶇<U+041E>锟?`
  L88 (2): `* @return 瑙掕壊鍒楄<U+3003>锛堝寘锟絠d, name 瀛楁<U+E18C>锟?`
  ... 15 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/sync/executor/DeptSyncExecutor.java` - 6 mojibake chars

  L11 (1): `* 閮<U+3129>棬鍚屾<U+E11E>鎵<U+0446><U+E511>锟?`
  L13 (1): `* 鐙<U+E102>珛锟紹ean锛岀敤浜庢墽琛岃法搴撲簨鍔<U+2103>搷浣滐拷`
  L24 (2): `* 鍚屾<U+E11E>閮<U+3129>棬淇<U+2103>伅锟絘udit 锟?`
  L38 (2): `* 鍚屾<U+E11E>閮<U+3129>棬淇<U+2103>伅锟絘pproval 锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/sync/executor/RoleSyncExecutor.java` - 8 mojibake chars

  L11 (1): `* 瑙掕壊鍚屾<U+E11E>鎵<U+0446><U+E511>锟?`
  L13 (1): `* 鐙<U+E102>珛锟紹ean锛岀敤浜庢墽琛岃法搴撲簨鍔<U+2103>搷浣滐拷`
  L14 (1): `* 閬垮厤 @Transactional 鑷<U+E047>皟鐢<U+3129>棶锟?`
  L24 (2): `* 鍚屾<U+E11E>瑙掕壊淇<U+2103>伅锟絘pproval 锟?`
  L33 (1): `// 鏇存柊鍖呭惈璇<U+30E8><U+E757>鑹茬殑瀹<U+2103>壒璁板綍锟絩ole_names 鏁扮粍`
  ... 1 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/sync/executor/UserSyncExecutor.java` - 8 mojibake chars

  L15 (1): `* 鐢<U+3126>埛鍚屾<U+E11E>鎵<U+0446><U+E511>锟?`
  L17 (1): `* 鐙<U+E102>珛锟紹ean锛岀敤浜庢墽琛岃法搴撲簨鍔<U+2103>搷浣滐拷`
  L18 (1): `* 閬垮厤 @Transactional 鑷<U+E047>皟鐢<U+3129>棶锟?`
  L31 (2): `* 鍚屾<U+E11E>鐢<U+3126>埛淇<U+2103>伅锟絧ermission 锟?`
  L48 (1): `* 鍚屾<U+E11E>鐢<U+3126>埛淇<U+2103>伅锟給rg 搴擄紙璐熻矗浜轰俊鎭<U+E224>級`
  ... 1 more lines

### `scm-system/service/src/main/java/com/scmcloud/system/sync/handler/DeptSyncHandler.java` - 4 mojibake chars

  L14 (1): `* 閮<U+3129>棬鏁版嵁鍚屾<U+E11E>澶勭悊锟?`
  L54 (3): `// 閫氳繃鐙<U+E102>珛锟紹ean 璋冪敤锛岀<U+2018>锟紷Transactional 锟紷DS 鐢熸晥`

### `scm-system/service/src/main/java/com/scmcloud/system/sync/handler/RoleSyncHandler.java` - 1 mojibake chars

  L14 (1): `* 瑙掕壊鏁版嵁鍚屾<U+E11E>澶勭悊锟?`

### `scm-system/service/src/main/java/com/scmcloud/system/sync/handler/UserSyncHandler.java` - 9 mojibake chars

  L18 (1): `* 鐢<U+3126>埛鏁版嵁鍚屾<U+E11E>澶勭悊锟?`
  L69 (3): `* 閫氳繃鐙<U+E102>珛锟紹ean 鍚屾<U+E11E>鐢<U+3126>埛淇<U+2103>伅锛岀<U+2018>锟紷Transactional 锟紷DS 鐢熸晥`
  L98 (1): `// 1. 鑾峰彇鎵<U+20AC>鏈夋湁瑙掕壊鍏宠仈鐨勭敤锟絀D`
  L104 (2): `// 2. 锟絬ser 搴撹幏鍙栫敤鎴蜂俊锟?`
  L115 (1): `// 鐢<U+3126>埛宸插垹闄<U+308F>紝浣嗚<U+E757>鑹插叧鑱旇繕锟?`
  ... 1 more lines

### `scm-system/service/src/test/java/com/scmcloud/system/service/command/DeptRoleCrossDatabaseCommandServiceTest.java` - 3 mojibake chars

  L42 (1): `// 閮<U+3129>棬-瑙掕壊鍏宠仈鍐欐搷浣滄祴锟?`
  L260 (1): `// 鎬<U+0446>兘鍜屽苟鍙戞祴锟?`
  L332 (1): `// 鏁版嵁涓<U+20AC>鑷存<U+20AC><U+0444>祴锟?`

### `scm-system/service/src/test/java/com/scmcloud/system/service/command/UserRoleCrossDatabaseCommandServiceTest.java` - 2 mojibake chars

  L55 (1): `// 鐢<U+3126>埛瑙掕壊鍏宠仈鍐欐搷浣滄祴锟?`
  L259 (1): `// 涓存椂瑙掕壊绠<U+FF04>悊鍐欐搷浣滄祴锟?`

### `scm-system/service/src/test/java/com/scmcloud/system/service/query/DeptCrossDatabaseQueryServiceTest.java` - 6 mojibake chars

  L74 (1): `// 閮<U+3129>棬鏍戞煡璇<U+3221>祴锟?`
  L310 (1): `when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(3); // 鏈<U+E104>儴锟?`
  L326 (1): `when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(3); // 鏈<U+E104>儴锟?`
  L343 (1): `when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(4); // 鏈<U+E104>儴闂<U+3125>強瀛愰儴锟?`
  L361 (1): `when(userRoleMapper.getUserDataScope(testUserId)).thenReturn(4); // 鏈<U+E104>儴闂<U+3125>強瀛愰儴锟?`
  ... 1 more lines

### `scm-system/service/src/test/java/com/scmcloud/system/service/query/UserCrossDatabaseQueryServiceTest.java` - 2 mojibake chars

  L376 (1): `assertEquals(5, result); // Default: 浠呮湰锟?`
  L389 (1): `assertEquals(5, result); // Default: 浠呮湰锟?`


## scm-tenant (19 files)

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/domain/entity/Tenant.java` - 1 mojibake chars

  L18 (1): `* 绉熸埛锟?`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/domain/entity/TenantConfig.java` - 1 mojibake chars

  L16 (1): `* 绉熸埛閰嶇疆锟?`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/domain/entity/TenantOperationLog.java` - 1 mojibake chars

  L18 (1): `* 绉熸埛鎿嶄綔鏃<U+30E5>織琛<U+E7D2>紙鍒嗗尯锟? </p>`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/domain/entity/TenantPackage.java` - 1 mojibake chars

  L16 (1): `* 绉熸埛濂楅<U+E635>锟?`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/domain/entity/TenantResourceQuota.java` - 1 mojibake chars

  L18 (1): `* 绉熸埛璧勬簮閰嶉<U+E582>锟?`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/domain/entity/TenantSubscription.java` - 1 mojibake chars

  L18 (1): `* 绉熸埛璁<U+3224>槄锟?`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/mapper/TenantConfigMapper.java` - 1 mojibake chars

  L8 (1): `* 绉熸埛閰嶇疆锟組apper 鎺<U+30E5>彛`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/mapper/TenantMapper.java` - 1 mojibake chars

  L8 (1): `* 绉熸埛锟組apper 鎺<U+30E5>彛`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/mapper/TenantOperationLogMapper.java` - 1 mojibake chars

  L8 (1): `* 绉熸埛鎿嶄綔鏃<U+30E5>織锟組apper 鎺<U+30E5>彛`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/mapper/TenantPackageMapper.java` - 1 mojibake chars

  L8 (1): `* 绉熸埛濂楅<U+E635>锟組apper 鎺<U+30E5>彛`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/mapper/TenantResourceQuotaMapper.java` - 1 mojibake chars

  L8 (1): `* 绉熸埛璧勬簮閰嶉<U+E582>锟組apper 鎺<U+30E5>彛`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/mapper/TenantSubscriptionMapper.java` - 1 mojibake chars

  L8 (1): `* 绉熸埛璁<U+3224>槄锟組apper 鎺<U+30E5>彛`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/service/ITenantConfigService.java` - 1 mojibake chars

  L9 (1): `* 绉熸埛閰嶇疆锟芥湇鍔<U+2605>拷`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/service/ITenantFeatureService.java` - 1 mojibake chars

  L11 (1): `* 绉熸埛鍔熻兘寮<U+20AC>鍏宠<U+3003> 鏈嶅姟锟?`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/service/ITenantOperationLogService.java` - 1 mojibake chars

  L11 (1): `* 绉熸埛鎿嶄綔鏃<U+30E5>織锟芥湇鍔<U+2605>拷 * </p>`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/service/ITenantPackageService.java` - 1 mojibake chars

  L11 (1): `* 绉熸埛濂楅<U+E635>锟芥湇鍔<U+2605>拷`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/service/ITenantResourceQuotaService.java` - 1 mojibake chars

  L11 (1): `* 绉熸埛璧勬簮閰嶉<U+E582>锟芥湇鍔<U+2605>拷`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/service/ITenantService.java` - 1 mojibake chars

  L11 (1): `* 绉熸埛锟芥湇鍔<U+2605>拷`

### `scm-tenant/service/src/main/java/com/scmcloud/tenant/service/ITenantSubscriptionService.java` - 1 mojibake chars

  L11 (1): `* 绉熸埛璁<U+3224>槄锟芥湇鍔<U+2605>拷`
