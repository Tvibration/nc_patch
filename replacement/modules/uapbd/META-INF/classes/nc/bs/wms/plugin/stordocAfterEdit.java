package nc.bs.wms.plugin;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import nc.bs.businessevent.IBusinessEvent;
import nc.bs.businessevent.IBusinessListener;
import nc.bs.businessevent.bd.BDCommonEvent;
import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.wms.tool.WMSHttpTool;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.vo.bd.material.MaterialVO;
import nc.vo.bd.stordoc.StordocVO;
import nc.vo.org.DeptVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.jdbc.framework.processor.ColumnProcessor;

public class stordocAfterEdit implements IBusinessListener {

	@Override
	public void doAction(IBusinessEvent arg0) throws BusinessException {
		// TODO Auto-generated method stub
		if (arg0 instanceof BDCommonEvent) {
			BDCommonEvent be = (BDCommonEvent) arg0;
			Object[] newObjs = be.getNewObjs();
			List<Map> mapList = new ArrayList();
			List<Map> mapList2 = new ArrayList();
			Map hmap = new HashMap();
			BaseDAO dao = new BaseDAO();
			dao.setAddTimeStamp(false);
			for (int i = 0; i < newObjs.length; i++) {
				if (newObjs[i] instanceof StordocVO) {
					StordocVO ov = (StordocVO)newObjs[i];
					String pk_org = ov.getPk_org();
					String wmsFlag = (String)dao.executeQuery("select s.value from pub_sysinit s where s.initcode = 'WMS' and s.pk_org='"+pk_org+"'", new ColumnProcessor());  //通过参数控制是否传WMS
					if(wmsFlag==null||wmsFlag.equals("N"))
						continue;
					String code = ov.getCode();
					String name = ov.getName();
					String isLocationControl = ov.getCsflag().booleanValue() ? "1" : "0";
					ov.getCsflag();
					int enablestate = ov.getEnablestate();
					if(enablestate==1)
						enablestate=0;
					else if(enablestate==2)
						enablestate=1;
					else
						enablestate=2;
					String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					String addr = (String)dao.executeQuery("select a.detailinfo from bd_address a where a.pk_address='"+ov.getStoraddr()+"'", new ColumnProcessor());
					
					Map map = new HashMap();
//					map.put("id", id);
					map.put("code", code);
					map.put("name", name);
					map.put("detailAddress", addr);
					map.put("principalName", ov.getPrincipalcode());
					map.put("principalTel", ov.getPhone());
					map.put("orgSrcSystemId", pk_org);
					map.put("orgCode", orgCode);
					map.put("orgName", orgName);
					map.put("enableStatus", enablestate);
					map.put("syncTime",(new UFDate(System.currentTimeMillis())).toString());
					map.put("create_time",(new UFDate(System.currentTimeMillis())).toString());
					map.put("syncBy",InvocationInfoProxy.getInstance().getUserCode());
					map.put("srcSystem", "NC");
					map.put("srcSystemId", ov.getPk_stordoc());
					map.put("srcSystemCode", code);
					map.put("srcSystemName", name);
					map.put("isLocationControl", isLocationControl);
					mapList.add(map);
				}
			}
			if(mapList!=null&&mapList.size()>0) {
//				String sessionID = WMSHttpTool.getsessionID();
//				hmap.put("lines", mapList);
//				Map map2 = new HashMap();
//				map2.put("params", hmap);
//				mapList2.add(map2);
				JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
//				String jsons = js.toString().substring(1, js.toString().length()-1);
				String jsons = js.toString();
				String url = WMSHttpTool.getWMSURL()+"/base/warehouse/ds/save-batch-by-id";
				try {
					String res = WMSHttpTool.sendPost(jsons, url);
					if(res.contains("\"success\":true")) {
						NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "bd_stordoc", "NC同步WMS仓库");
					}else
						throw new BusinessException(res);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "bd_stordoc", "NC同步WMS仓库");
					throw new BusinessException(e.toString());
				}
			}
		}
	}

}
