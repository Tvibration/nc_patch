package nc.bs.pub.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.NCLocator;
import nc.bs.pubapp.pf.action.AbstractPfAction;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.bs.wms.tool.WMSHttpTool;
import nc.impl.pubapp.pattern.rule.processer.CompareAroundProcesser;
import nc.itf.mmpac.pickm.IPickmMaintainService;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.vo.mmpac.pickm.entity.AggPickmVO;
import nc.vo.mmpac.pickm.entity.PickmHeadVO;
import nc.vo.mmpac.pickm.entity.PickmItemVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOAggVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOHeadVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOItemVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.compiler.PfParameterVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nccloud.vo.mmpub.utils.power.MMDataPermissionUtil;

public class N_55A3_APPROVE extends AbstractPfAction<AggPickmVO>
{
  protected CompareAroundProcesser<AggPickmVO> getCompareAroundProcesserWithRules(Object userObj)
  {
    return null;
  }

  protected AggPickmVO[] processBP(Object userObj, AggPickmVO[] clientFullVOs, AggPickmVO[] originBills)
  {
    try {
      if (this.m_tmpVo.isCloudEntry)
      {
        if (clientFullVOs[0].getParentVO().getFprodmode().intValue() == 2) {
          MMDataPermissionUtil.checkPermission(clientFullVOs, "55C3", "DPickmAudit", "vbillcode");
        }
        else
        {
          MMDataPermissionUtil.checkPermission(clientFullVOs, "55A3", "PPickmAdudit", "vbillcode");
        }
      }

      for (AggPickmVO aggPickm : clientFullVOs) {
        aggPickm.getParentVO().setStatus(1);
      }
      sendWMS(clientFullVOs);
      return ((IPickmMaintainService)NCLocator.getInstance().lookup(IPickmMaintainService.class)).auditPickm(clientFullVOs);
    }
    catch (BusinessException e) {
      ExceptionUtils.wrappException(e);
    }
    return null;
  }
  
  /**
   * 备料计划传WMS
   * zhoush
   * 20240228
   */
  private void sendWMS(AggPickmVO[] pickVOs) throws BusinessException{
	  BaseDAO dao = new BaseDAO();
	  List<Map> mapList = new ArrayList();
	  for(int i=0;i<pickVOs.length;i++) {
		  
		  AggPickmVO cv = pickVOs[i];
		  PickmHeadVO phv = cv.getParentVO();
		  PickmItemVO[] pbv = (PickmItemVO[])cv.getChildrenVO();
		  String pk_org = phv.getPk_org();
		  String mesFlag = (String)dao.executeQuery("select s.value from pub_sysinit s where s.initcode = 'WMS' and s.pk_org='"+pk_org+"'", new ColumnProcessor());  //通过参数控制是否传WMS
		  if(mesFlag==null||mesFlag.equals("N"))
				continue;
		  String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
		  String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
		  String Billmaker = (String)dao.executeQuery("select u.user_name from sm_user u where u.cuserid='"+phv.getBillmaker()+"'", new ColumnProcessor());
		  
		  Map map = new HashMap();
		  String departmentName = (String)dao.executeQuery("select d.name from org_dept d where d.pk_dept='"+phv.getCdeptid()+"'", new ColumnProcessor());
		  String customerCode = (String)dao.executeQuery("select c.code from bd_customer c where c.pk_customer='"+phv.getCcustomerid()+"'", new ColumnProcessor());
		  String customerName = (String)dao.executeQuery("select c.name from bd_customer c where c.pk_customer='"+phv.getCcustomerid()+"'", new ColumnProcessor());
		  map.put("srcSystemId",phv.getCpickmid());
		  map.put("code",phv.getVbillcode());
		  map.put("billDate",phv.getDmakedate().toString());
		  map.put("delFlag",0);
		  map.put("orgIdSrcSystemId",pk_org);
		  map.put("orgCode",orgCode);
		  map.put("orgName",orgName);
		  map.put("remark",phv.getVnote());
		  map.put("makingPersonName",Billmaker);
		  map.put("makingBillDate", phv.getDmakedate().toString());
		  map.put("departmentName",departmentName);
		  map.put("productionOrderSrcSystemId",phv.getCsourcebillid());
		  map.put("productionOrderLine",phv.getVsourcebillrowno());
		  map.put("productSrcSystemId",phv.getCmaterialid());
		  map.put("productBatchCode",phv.getVbatchcode());
		  map.put("mainUnitSrcSystemId", phv.getCunitid());
		  map.put("auxiliaryUnitSrcSystemId", phv.getCastunitid());
		  map.put("conversionRate", phv.getVchangerate());
		  map.put("plannedNum", phv.getNastnum().toString());
		  map.put("plannedMainNum", phv.getNnumber().toString());
		  List<Map> mapList2 = new ArrayList();
		  for(int j=0;j<pbv.length;j++) {
			  Map map_b = new HashMap();
			  map_b.put("srcSystemId", pbv[j].getCpickm_bid());
			  map_b.put("lineNo", pbv[j].getVrowno());
			  map_b.put("distributionMaterialWarehouseSrcSystemId", pbv[j].getCoutstockid());
			  map_b.put("remark", pbv[j].getVbnote());
			  map_b.put("productSrcSystemId", pbv[j].getCbmaterialid());
			  map_b.put("mainUnitSrcSystemId", pbv[j].getCbunitid());
			  map_b.put("auxiliaryUnitSrcSystemId", pbv[j].getCbastunitid());
			  map_b.put("plannedDeliveryNum", pbv[j].getNplanoutastnum().toString());
			  map_b.put("plannedDeliveryMainNum", pbv[j].getNplanoutnum().toString());
			  map_b.put("conversionRate", pbv[j].getVbchangerate());
			  map_b.put("batchCode", pbv[j].getVbatchcode());
			  map_b.put("workshopSrcSystemId",pbv[j].getCwkid());
			  int conversemethod = (int)dao.executeQuery("select p.conversemethod from bd_materialprod p where p.pk_material='"+pbv[j].getCbmaterialid()+"' and p.pk_org='"+pk_org+"'", new ColumnProcessor());
			  if(conversemethod==1)
				  map_b.put("ext01","N");
			  else
				  map_b.put("ext01","Y");
			  map_b.put("delFlag", 0);
			  mapList2.add(map_b);
		  }
		  map.put("materialPreparationPlanItemList", mapList2);
		  mapList.add(map);
	  }
	  if(mapList!=null&&mapList.size()>0) {
			JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
//			String jsons = js.toString().substring(1, js.toString().length()-1);
			String jsons = js.toString();
			String url = WMSHttpTool.getWMSURL()+"/production/material-preparation-plan/ds/save-batch-by-id";
			try {
				String res = WMSHttpTool.sendPost(jsons, url);
				if(res.contains("\"success\":true")) {
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, url, "mm_pickm", "NC备料计划同步WMS");
				}else
					throw new BusinessException(res);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, url, "mm_pickm", "NC备料计划同步WMS");
				throw new BusinessException(e.toString());
			}
		}
  }
}