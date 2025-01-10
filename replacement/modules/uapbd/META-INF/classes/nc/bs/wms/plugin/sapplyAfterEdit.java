package nc.bs.wms.plugin;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import nc.bs.businessevent.BusinessEvent;
import nc.bs.businessevent.IBusinessEvent;
import nc.bs.businessevent.IBusinessListener;
import nc.bs.businessevent.bd.BDCommonEvent;
import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.ic.general.businessevent.ICGeneralCommonEvent;
import nc.bs.wms.tool.WMSHttpTool;
import nc.itf.pubapp.pub.smart.IBillQueryService;
import nc.bs.uapbd.tool.HttpTool;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.vo.bd.material.MaterialVO;
import nc.vo.ic.m4455.entity.SapplyBillBodyVO;
import nc.vo.ic.m4455.entity.SapplyBillHeadVO;
import nc.vo.ic.m4455.entity.SapplyBillVO;
import nc.vo.ic.m4d.entity.MaterialOutBodyVO;
import nc.vo.ic.m4d.entity.MaterialOutHeadVO;
import nc.vo.ic.m4d.entity.MaterialOutVO;
import nc.vo.ic.m4i.entity.GeneralOutBodyVO;
import nc.vo.ic.m4i.entity.GeneralOutHeadVO;
import nc.vo.ic.m4i.entity.GeneralOutVO;
import nc.vo.ic.m4k.entity.WhsTransBillBodyVO;
import nc.vo.ic.m4k.entity.WhsTransBillHeaderVO;
import nc.vo.mmpac.pickm.entity.AggPickmVO;
import nc.vo.mmpac.pickm.entity.PickmHeadVO;
import nc.vo.mmpac.pickm.entity.PickmItemVO;
import nc.vo.org.DeptVO;
import nc.vo.pu.m21.entity.OrderHeaderVO;
import nc.vo.pu.m21.entity.OrderItemVO;
import nc.vo.pu.m21.entity.OrderVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.jdbc.framework.processor.ColumnListProcessor;
import nc.jdbc.framework.processor.ColumnProcessor;

public class sapplyAfterEdit implements IBusinessListener {

	@Override
	public void doAction(IBusinessEvent arg0) throws BusinessException {
		// TODO 自动生成的方法存根
		if (arg0 instanceof ICGeneralCommonEvent) {
			ICGeneralCommonEvent be = (ICGeneralCommonEvent) arg0;
			Object newObjs = be.getOldObjs();
			List<Map> mapList = new ArrayList();
//			List<Map> mapList2 = new ArrayList();
			Map hmap = new HashMap();
			BaseDAO dao = new BaseDAO();
			dao.setAddTimeStamp(false);
			IBillQueryService IQ = (IBillQueryService) NCLocator.getInstance().lookup(IBillQueryService.class);
			if (newObjs instanceof SapplyBillVO[]) {
				SapplyBillVO[] ovs = (SapplyBillVO[])newObjs;
				for(int i=0;i<ovs.length;i++) {
					  SapplyBillVO svo = ovs[i];
//					  String hid = svo.getHead().getCgeneralhid();
//					  String cgeneralhid = (String)dao.executeQuery("select b.cgeneralhid from ic_generalout_b b where b.csourcebillhid='"+hid+"' and b.dr=0", new ColumnProcessor());
//					  GeneralOutVO cv = IQ.querySingleBillByPk(GeneralOutVO.class, cgeneralhid);
					  SapplyBillHeadVO phv = svo.getHead();
					  SapplyBillBodyVO[] pbv = (SapplyBillBodyVO[])svo.getChildrenVO();
					  String pk_org = phv.getPk_org();
					  String wmsFlag = (String)dao.executeQuery("select s.value from pub_sysinit s where s.initcode = 'WMS' and s.pk_org='"+pk_org+"'", new ColumnProcessor());  //通过参数控制是否传WMS
						if(wmsFlag==null||wmsFlag.equals("N"))
							continue;
					  String ctrantypeid = phv.getCtrantypeid();
//					  if(!ctrantypeid.equals("1001A2100000004MZC6P"))   //研发赠品 正式环境
					  if(!ctrantypeid.equals("1001A7100000004FPAAS"))   //研发赠品 测试环境
						  continue;
//					  if(ctrantypeid!=null&&ctrantypeid.equals("1001A7100000004FPAAS"))   //测试环境
					  String Billmaker = (String)dao.executeQuery("select p.code from sm_user u inner join bd_psndoc p on u.pk_psndoc=p.pk_psndoc where u.cuserid='"+phv.getBillmaker()+"'", new ColumnProcessor());
					  String lybm = (String)dao.executeQuery("select d.name from org_dept d where d.pk_dept='"+phv.getCdptid()+"'", new ColumnProcessor());
					  Map map = new HashMap();
					  map.put("srcSystemId",phv.getCgeneralhid());
					  map.put("code",phv.getVbillcode());
					  map.put("billDate",phv.getDbilldate().toString());
					  map.put("delFlag",0);
					  map.put("orgSrcSystemId",pk_org);
					  map.put("warehouseSrcSystemId",pbv[0].getCwarehouseid());
					  map.put("departmentName",lybm);

					  map.put("orderTypeName","赠品出库");
					  map.put("isBack","N");
					  map.put("remark",phv.getVnote());
					  map.put("createBy",Billmaker);
					  map.put("createTime", phv.getDmakedate().toString());
					  
					  List<Map> mapList2 = new ArrayList();
					  for(int j=0;j<pbv.length;j++) {
						  Map map_b = new HashMap();
						  map_b.put("srcSystemId", pbv[j].getCgeneralbid());
						  map_b.put("lineNo", pbv[j].getCrowno());
						  map_b.put("remark", pbv[j].getVnotebody());
						  map_b.put("productSrcSystemId", pbv[j].getCmaterialoid());
						  map_b.put("orderCode", phv.getVbillcode());
						  map_b.put("batchCode", pbv[j].getVbatchcode());
						  map_b.put("astNum", pbv[j].getNassistnum().toString());  //传应发数，WMS处理后再回写NC实发数
						  map_b.put("mainNum", pbv[j].getNnum().toString()); 
						  map_b.put("mainUnitSrcSystemId", pbv[j].getCunitid());
						  map_b.put("astUnitSrcSystemId", pbv[j].getCastunitid());
						  map_b.put("rate", pbv[j].getVchangerate());
						  map_b.put("warehouseSrcSystemId", pbv[j].getCwarehouseid());
						  map_b.put("delFlag", 0);
//						  map_b.put("cworkcenterid", pbv[j].getCworkcenterid());
						  mapList2.add(map_b);
					  }
					  map.put("stockMaterialOutDetailList", mapList2);
					  mapList.add(map);
				  }
				  if(mapList!=null&&mapList.size()>0) {
						JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
//						String jsons = js.toString().substring(1, js.toString().length()-1);
						String jsons = js.toString();
						String url = WMSHttpTool.getWMSURL()+"/stock/stock-material-out/ds/save-batch-by-id";
						try {
							String res = WMSHttpTool.sendPost(jsons, url);
							if(res.contains("\"success\":true")) {
								NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "ic_sapply_h", "NC出库申请同步WMS");
							}else
								throw new BusinessException(res);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "ic_sapply_h", "NC出库申请同步WMS");
							throw new BusinessException(e.toString());
						}
					}	  
				
			}		
		}     
	
	}

}
