package nc.bs.wms.plugin;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import nc.bs.businessevent.BdUpdateEvent;
import nc.bs.businessevent.IBusinessEvent;
import nc.bs.businessevent.IBusinessListener;
import nc.bs.businessevent.bd.BDCommonEvent;
import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.wms.tool.WMSHttpTool;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.vo.bd.material.MaterialVO;
import nc.vo.bd.material.measdoc.MeasdocVO;
import nc.vo.bd.psn.PsndocVO;
import nc.vo.bd.stordoc.StordocVO;
import nc.vo.org.DeptVO;
import nc.vo.pu.m23.entity.ArriveHeaderVO;
import nc.vo.pu.m23.entity.ArriveItemVO;
import nc.vo.pu.m23.entity.ArriveVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.jdbc.framework.processor.ColumnProcessor;

public class arriveAfterEdit implements IBusinessListener {

	@Override
	public void doAction(IBusinessEvent arg0) throws BusinessException {
		// TODO Auto-generated method stub
		if (arg0 instanceof BdUpdateEvent) {
			BdUpdateEvent be = (BdUpdateEvent) arg0;
			Object newObjs = be.getNewObject();
			List<Map> mapList = new ArrayList();
			List<Map> mapList2 = new ArrayList();
			Map hmap = new HashMap();
			BaseDAO dao = new BaseDAO();
			dao.setAddTimeStamp(false);

			if (newObjs instanceof ArriveVO[]) {
				ArriveVO[] vos = (ArriveVO[])newObjs;
				for(int i=0;i<vos.length;i++) {
					ArriveVO ov = vos[i];
					ArriveHeaderVO ohv = ov.getHVO();
					ArriveItemVO[] obvs = ov.getBVO();
					String pk_org = ohv.getPk_org();
					String creater = (String)dao.executeQuery("select u.user_name from sm_user u where u.cuserid='"+ohv.getCreator()+"'", new ColumnProcessor());  //创建人
					String mesFlag = (String)dao.executeQuery("select s.value from pub_sysinit s where s.initcode = 'WMS' and s.pk_org='"+pk_org+"'", new ColumnProcessor());  //通过参数控制是否传WMS
					if(mesFlag==null||mesFlag.equals("N"))
						return;
					String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					
					Map map = new HashMap();
					map.put("srcSystemId", ohv.getPk_arriveorder());
					map.put("code", ohv.getVbillcode());
					map.put("billDate", ohv.getDbilldate().toString());
					map.put("delFlag", 0);
					map.put("traderSrcSystemId", ohv.getPk_supplier());
					map.put("orgSrcSystemId", ohv.getPk_org());
					map.put("remark", ohv.getVmemo());
					map.put("createBy", creater);
					map.put("createTime", ohv.getCreationtime().toString());
					for(int r=0;r<obvs.length;r++) {
						Map map_b = new HashMap();
						map_b.put("srcSystemId", obvs[r].getPk_arriveorder_b());
						map_b.put("lineNo", obvs[r].getCrowno());
						map_b.put("productSrcSystemId", obvs[r].getPk_material());
						map_b.put("orderCode", ohv.getVbillcode());
						map_b.put("batchCode", obvs[r].getVbatchcode());
						map_b.put("mainNum", obvs[r].getNnum().toString());
						map_b.put("astNum", obvs[r].getNastnum().toString());
						map_b.put("mainUnitSrcSystemId", obvs[r].getCunitid());
						map_b.put("astUnitSrcSystemId", obvs[r].getCastunitid());
						map_b.put("rate", obvs[r].getVchangerate());
						map_b.put("warehouseSrcSystemId", obvs[r].getPk_receivestore());
						map_b.put("remark", obvs[r].getVmemob());
						map_b.put("delFlag", 0);
						mapList2.add(map_b);
					}
					map.put("arriveDetailList", mapList2);
					mapList.add(map);
				}
				
			}	
			
			if(mapList!=null&&mapList.size()>0) {

				JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
				String jsons = js.toString();
				String url = WMSHttpTool.getWMSURL()+"/purchase/arrive-order/ds/save-batch-by-id";
				try {
					String res = WMSHttpTool.sendPost(jsons, url);
					if(res.contains("\"success\":true")) {
						NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, url, "po_arriveorder", "NC同步WMS到货单");
					}else
						throw new BusinessException(res);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, url, "po_arriveorder", "NC同步WMS到货单");
					throw new BusinessException(e.toString());
				}
			}
		}
	}

}
