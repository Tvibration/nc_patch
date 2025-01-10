package nc.bs.pub.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.mmpac.pmo.pac0002.pluginpoint.PMOPluginPoint;
import nc.bs.pubapp.pf.action.AbstractPfAction;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.bs.wms.tool.WMSHttpTool;
import nc.impl.pubapp.pattern.rule.processer.CompareAroundProcesser;
import nc.itf.mmpac.pmo.pac0002.IPMOMaintainService;
import nc.jdbc.framework.processor.ArrayListProcessor;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.vo.mmpac.pmo.pac0002.entity.PMOAggVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOHeadVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOItemVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.compiler.PfParameterVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nc.vo.scmf.dm.carrier.entity.CarrierVO;
import nccloud.vo.mmpub.utils.power.MMDataPermissionUtil;

//mmpac模块
public class N_55A2_APPROVE extends AbstractPfAction<PMOAggVO>
{ 
  protected CompareAroundProcesser<PMOAggVO> getCompareAroundProcesserWithRules(Object userObj)
  {
    CompareAroundProcesser processer = new CompareAroundProcesser(PMOPluginPoint.APPROVE);

    return null;
  }

  protected PMOAggVO[] processBP(Object userObj, PMOAggVO[] clientFullVOs, PMOAggVO[] originBills)
  {
    IPMOMaintainService srv = (IPMOMaintainService)NCLocator.getInstance().lookup(IPMOMaintainService.class);
    try {
      if (this.m_tmpVo.isCloudEntry)
      {
        MMDataPermissionUtil.checkPermission(clientFullVOs, "55A2", "pmoAudit", "vbillcode");
      }
      sendWMS(clientFullVOs);
      return srv.approve(clientFullVOs);
    } catch (BusinessException e) {
      ExceptionUtils.wrappException(e); }
    return null;
  }
  
  /**
   * 生产订单传WMS
   * zhoush
   * 20240208
   */
  private void sendWMS(PMOAggVO[] pmoVOs) throws BusinessException{
	  BaseDAO dao = new BaseDAO();
	  List<Map> mapList = new ArrayList();
	  for(int i=0;i<pmoVOs.length;i++) {
		  
		  PMOAggVO cv = pmoVOs[i];
		  PMOHeadVO phv = cv.getParentVO();
		  PMOItemVO[] pbv = cv.getChildrenVO();
		  String pk_org = phv.getPk_org();
		  String mesFlag = (String)dao.executeQuery("select s.value from pub_sysinit s where s.initcode = 'WMS' and s.pk_org='"+pk_org+"'", new ColumnProcessor());  //通过参数控制是否传WMS
		  if(mesFlag==null||mesFlag.equals("N"))
				continue;
		  String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
		  String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
		  String Billmaker = (String)dao.executeQuery("select u.user_name from sm_user u where u.cuserid='"+phv.getBillmaker()+"'", new ColumnProcessor());
		  for(int j=0;j<pbv.length;j++) {
			  Map map = new HashMap();
			  String departmentName = (String)dao.executeQuery("select d.name from org_dept d where d.pk_dept='"+pbv[j].getCdeptid()+"'", new ColumnProcessor());
			  String customerCode = (String)dao.executeQuery("select c.code from bd_customer c where c.pk_customer='"+pbv[j].getCcustomerid()+"'", new ColumnProcessor());
			  String customerName = (String)dao.executeQuery("select c.name from bd_customer c where c.pk_customer='"+pbv[j].getCcustomerid()+"'", new ColumnProcessor());
			  map.put("srcSystemId",pbv[j].getCmoid());
			  map.put("orderSrcSystemId",phv.getCpmohid());
			  map.put("code",phv.getVbillcode());
			  map.put("billDate",phv.getDbilldate().toString());
			  map.put("delFlag",0);
			  map.put("orgIdSrcSystemId",pk_org);
			  map.put("orgCode",orgCode);
			  map.put("orgName",orgName);
			  map.put("remark",phv.getVnote());
			  map.put("orderLine",pbv[j].getVrowno());
			  map.put("orderLineSrcSystemId",pbv[j].getCmoid());
			  map.put("makingPersonName",Billmaker);
			  map.put("makingBillDate", phv.getDmakedate().toString());
			  map.put("productionDeptSrcSystemId", pbv[j].getCdeptid());
			  map.put("departmentName",departmentName);
			  map.put("customerIdSrcSystemId", pbv[j].getCcustomerid());
			  map.put("customerCode", customerCode);
			  map.put("customerName", customerName);
			  map.put("businessSourceOrderCode", pbv[j].getVsalebillcode());
			  map.put("businessSourcePlanOrderCode", pbv[j].getVsrccode());
			  map.put("productSrcSystemId", pbv[j].getCmaterialid());
			  map.put("productBatchCode", pbv[j].getVbatchcode());
			  map.put("mainUnitSrcSystemId", pbv[j].getCunitid());
			  map.put("auxiliaryUnitSrcSystemId", pbv[j].getCastunitid());
			  map.put("conversionRate", pbv[j].getVchangerate());
			  map.put("plannedInputNum", pbv[j].getNnum().toString());
			  map.put("plannedInputMainNum", pbv[j].getNastnum().toString());
			  map.put("num", pbv[j].getNnum().toString());
			  map.put("mainNum", pbv[j].getNastnum().toString());
			  map.put("manualNum", pbv[j].getVdef14());
			  map.put("workshopSrcSystemId", pbv[j].getCwkid());
			  map.put("typeIdSrcSystemId", pbv[j].getCteamid());
			  
			  String sql = "select p.cplanoutputid srcSystemId,p.vrowno lineNo,p.cmaterialid productSrcSystemId,p.cunitid mainUnitSrcSystemId,p.castunitid auxiliaryUnitSrcSystemId,"
			  		+ "p.nastplanoutputnum otherNum,p.nplanoutputnum otheMainNum,p.vchangerate conversionRate,(case when p.foutputtype=2 then 1 else 2 end) typeCode,p.dr delFlag from mm_mo_planoutput p "
			  		+ "where p.dr=0 and p.foutputtype in (2,3) and p.cmoid ='"+pbv[j].getCmoid()+"'";
				List<Object[]> results = (List<Object[]>) dao.executeQuery(sql, new ArrayListProcessor());
				List<Map> mapList2 = new ArrayList();
				for(int k=0;k<results.size();k++) {
					Map map_msv = new HashMap();
					map_msv.put("srcSystemId", String.valueOf(results.get(k)[0]));
					map_msv.put("lineNo", String.valueOf(results.get(k)[1]));
					map_msv.put("productSrcSystemId", String.valueOf(results.get(k)[2]));
					map_msv.put("mainUnitSrcSystemId", String.valueOf(results.get(k)[3]));
					map_msv.put("auxiliaryUnitSrcSystemId", results.get(k)[4]==null?"":String.valueOf(results.get(k)[4]));
					map_msv.put("otherNum",String.valueOf(results.get(k)[5]));
					map_msv.put("otheMainNum", String.valueOf(results.get(k)[6]));
					map_msv.put("conversionRate", String.valueOf(results.get(k)[7]));
					map_msv.put("factor", "0");
					map_msv.put("typeCode", String.valueOf(results.get(k)[8]));
					map_msv.put("srcSystemCode", String.valueOf(results.get(k)[9]));
					mapList2.add(map_msv);
				}
				map.put("productionOrderOtherItemList",mapList2);
			  
			  mapList.add(map);
		  }
		  
	  }
	  if(mapList!=null&&mapList.size()>0) {
			JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
//			String jsons = js.toString().substring(1, js.toString().length()-1);
			String jsons = js.toString();
			String url = WMSHttpTool.getWMSURL()+"/production/production-order/ds/save-batch-by-id";
			try {
				String res = WMSHttpTool.sendPost(jsons, url);
				if(res.contains("\"success\":true")) {
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "dm_carrier", "NC生产订单同步WMS");
				}else
					throw new BusinessException(res);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "dm_carrier", "NC生产订单同步WMS");
				throw new BusinessException(e.toString());
			}
		}
  }
}