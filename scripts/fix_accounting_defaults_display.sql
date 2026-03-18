-- 修復 Accounting Schema Defaults 顯示中文名稱
-- 針對 Client ID = 1000031 (天地人實業有限公司)
--
-- 問題說明：
-- C_ElementValue.Name 已經是中文，但 Accounting Schema > Defaults 視窗
-- 顯示的是 C_ValidCombination.Description，需要同步更新

-- 1. 先確認 C_ElementValue 的中文名稱
SELECT value, name FROM c_elementvalue
WHERE ad_client_id = 1000031
ORDER BY value LIMIT 10;

-- 2. 查看目前 C_ValidCombination.Description 的內容
SELECT vc.c_validcombination_id, vc.description, vc.alias, ev.value, ev.name
FROM c_validcombination vc
JOIN c_elementvalue ev ON vc.account_id = ev.c_elementvalue_id
WHERE vc.ad_client_id = 1000031
LIMIT 10;

-- 3. 更新 C_ValidCombination.Description 為「代碼-中文名稱」格式
UPDATE C_ValidCombination vc
SET Description = (
    SELECT ev.Value || '-' || ev.Name
    FROM C_ElementValue ev
    WHERE ev.C_ElementValue_ID = vc.Account_ID
)
WHERE vc.AD_Client_ID = 1000031
AND vc.Account_ID IS NOT NULL;

-- 4. 更新 C_ValidCombination.Alias 為中文名稱
UPDATE C_ValidCombination vc
SET Alias = (
    SELECT ev.Name
    FROM C_ElementValue ev
    WHERE ev.C_ElementValue_ID = vc.Account_ID
)
WHERE vc.AD_Client_ID = 1000031
AND vc.Account_ID IS NOT NULL;

-- 5. 驗證更新結果
SELECT vc.c_validcombination_id, vc.description, vc.alias, ev.value, ev.name
FROM c_validcombination vc
JOIN c_elementvalue ev ON vc.account_id = ev.c_elementvalue_id
WHERE vc.ad_client_id = 1000031
LIMIT 10;

-- 完成後請在 iDempiere WebUI 中重新開啟 Accounting Schema > Defaults 視窗
-- 如果仍未顯示，請清除瀏覽器快取或登出再登入
