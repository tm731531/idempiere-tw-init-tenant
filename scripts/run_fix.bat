@echo off
chcp 65001
set PGPASSWORD=adempiere
"D:\DEVTOOL\PGSQL\bin\psql.exe" -h 192.168.0.48 -p 5432 -U adempiere -d idempiere -c "UPDATE C_ValidCombination vc SET Description = (SELECT ev.Value || '-' || ev.Name FROM C_ElementValue ev WHERE ev.C_ElementValue_ID = vc.Account_ID) WHERE vc.AD_Client_ID = 1000031 AND vc.Account_ID IS NOT NULL;"

echo.
echo 更新 Alias...
"D:\DEVTOOL\PGSQL\bin\psql.exe" -h 192.168.0.48 -p 5432 -U adempiere -d idempiere -c "UPDATE C_ValidCombination vc SET Alias = (SELECT ev.Name FROM C_ElementValue ev WHERE ev.C_ElementValue_ID = vc.Account_ID) WHERE vc.AD_Client_ID = 1000031 AND vc.Account_ID IS NOT NULL;"

echo.
echo 驗證結果...
"D:\DEVTOOL\PGSQL\bin\psql.exe" -h 192.168.0.48 -p 5432 -U adempiere -d idempiere -c "SELECT vc.description, ev.value, ev.name FROM c_validcombination vc JOIN c_elementvalue ev ON vc.account_id = ev.c_elementvalue_id WHERE vc.ad_client_id = 1000031 LIMIT 5;"

pause
