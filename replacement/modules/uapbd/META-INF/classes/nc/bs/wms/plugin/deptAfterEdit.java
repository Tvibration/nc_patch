package nc.bs.wms.plugin;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nc.bs.businessevent.IBusinessEvent;
import nc.bs.businessevent.IBusinessListener;
import nc.bs.businessevent.bd.BDCommonEvent;
import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.ic.general.businessevent.ICGeneralCommonEvent;
import nc.bs.uapbd.tool.HttpTool;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.bs.wms.tool.WMSHttpTool;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.vo.ic.m4c.entity.SaleOutVO;
import nc.vo.om.hrdept.AggHRDeptVO;
import nc.vo.org.DeptVO;
import nc.vo.org.OrgVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;

import org.apache.commons.lang.ArrayUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class deptAfterEdit implements IBusinessListener {

	@Override
	public void doAction(IBusinessEvent event) throws BusinessException {
		// TODO 自动生成的方法存根

		// TODO 自动生成的方法存根
		if (event instanceof BDCommonEvent) {
			BDCommonEvent be = (BDCommonEvent) event;
			Object[] oldObjs = be.getOldObjs();
			Object[] newObjs = be.getNewObjs();
			List<Map> mapList = new ArrayList();
			List<Map> mapList2 = new ArrayList();
			Map hmap = new HashMap();
			BaseDAO dao = new BaseDAO();
			dao.setAddTimeStamp(false);
			for (int i = 0; i < newObjs.length; i++) {
				if (newObjs[i] instanceof DeptVO) {
					String url = WMSHttpTool.getWMSURL()+"/admin/dept/sync";
					String jsons = "";
					try {
						DeptVO ov = (DeptVO)newObjs[i];
						String pk_org = ov.getPk_org();
						String wmsFlag = (String)dao.executeQuery("select s.value from pub_sysinit s where s.initcode = 'WMS' and s.pk_org='"+pk_org+"'", new ColumnProcessor());  //通过参数控制是否传WMS
						if(wmsFlag==null||wmsFlag.equals("N"))
							continue;
						String creator = (String)dao.executeQuery("select p.pk_psndoc from sm_user u inner join bd_psndoc p on u.pk_psndoc=p.pk_psndoc where u.cuserid='"+ov.getCreator()+"'", new ColumnProcessor()); 
						String modifier = (String)dao.executeQuery("select p.pk_psndoc from sm_user u inner join bd_psndoc p on u.pk_psndoc=p.pk_psndoc where u.cuserid='"+ov.getModifier()+"'", new ColumnProcessor());
						Map map = new HashMap();
//						map.put("code", code);
						map.put("name", ov.getName());
						map.put("sortOrder", 999);
						map.put("createBy", creator);
						map.put("updateBy", modifier);
						map.put("createTime", (new UFDate(System.currentTimeMillis())).toString());
						map.put("updateTime", ov.getModifiedtime()==null?"":ov.getModifiedtime().toString());
						map.put("parentSrcSystemId", ov.getPk_fatherorg());
						map.put("srcSystem", "NC");
						map.put("srcSystemId", ov.getPk_dept());
						map.put("srcSystemCode", ov.getCode());
						map.put("srcSystemName", ov.getName());
						map.put("orgSrcSystemId", ov.getPk_org());
						map.put("delFlag", 0);
						mapList.add(map);
						
						JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
						jsons = js.toString().substring(1, js.toString().length()-1);
							String res = WMSHttpTool.sendPost(jsons, url);
							if(res.contains("\"success\":true")) {
								NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "org_dept", "NC同步WMS部门");
							}else
								throw new BusinessException(res);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "org_dept", "NC同步WMS部门");
						throw new BusinessException(e.toString());
					}
				}
			}

			
//			if(mapList!=null&&mapList.size()>0) {
//				String sessionID = HttpTool.getsessionID();
//				hmap.put("lines", mapList);
//				Map map2 = new HashMap();
//				map2.put("params", hmap);
//				mapList2.add(map2);
//				JSONArray js = (JSONArray)JSONObject.toJSON(mapList2);
//				String jsons = js.toString().substring(1, js.toString().length()-1);
//				String url = WMSHttpTool.getWMSURL()+"/admin/dept/sync";
//				try {
//					String res = WMSHttpTool.sendPost(jsons, url);
//					if(res.contains("ok")||res.contains("\"code\": 200")) {
//						NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, "", "org_dept", "部门");
//					}else
//						throw new BusinessException(res);
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, "", "org_dept", "部门");
//					throw new BusinessException(e.toString());
//				}
//			}
		}
        
	}

}
