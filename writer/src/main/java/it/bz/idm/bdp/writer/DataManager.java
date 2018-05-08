package it.bz.idm.bdp.writer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import it.bz.idm.bdp.dal.DataType;
import it.bz.idm.bdp.dal.MeasurementStringHistory;
import it.bz.idm.bdp.dal.Station;
import it.bz.idm.bdp.dal.authentication.BDPRole;
import it.bz.idm.bdp.dal.util.JPAUtil;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.StationDto;

@Component
public class DataManager {

	public Object pushRecords(String stationType, Object... data){
		EntityManager em = JPAUtil.createEntityManager();
		Station station;
		try {
			station = (Station) JPAUtil.getInstanceByType(em,stationType);
			return station.pushRecords(em, data);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if (em.isOpen())
				em.close();
		}
		return null;
	}
	public Object syncStations(String stationType, List<StationDto> dtos){
		EntityManager em = JPAUtil.createEntityManager();
		try{
			Station station = (Station) JPAUtil.getInstanceByType(em,stationType);
			return station.syncStations(em, dtos);
		} catch (Exception e) {
			// FIXME Add error handling to report back to the writer method caller
			e.printStackTrace();
		} finally {
			em.close();
		}
		return null;
	}

	public Object syncDataTypes(List<DataTypeDto> dtos) {
		Object object = null;
		EntityManager em = JPAUtil.createEntityManager();
		try{
			object = DataType.sync(em,dtos);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			em.close();
		}
		return object;
	}

	public Object getDateOfLastRecord(String stationtype,String stationcode,String type,Integer period){
		EntityManager em = JPAUtil.createEntityManager();
		BDPRole role = BDPRole.fetchAdminRole(em);
		Date date = new Date(-1);
		try{
			Station s = (Station) JPAUtil.getInstanceByType(em, stationtype);
			Station station = s.findStation(em,stationcode);
			DataType dataType = DataType.findByCname(em,type);
			if (station != null) {
				date = station.getDateOfLastRecord(em, station, dataType, period, role);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		finally{
			em.close();
		}
		return date;
	}

	public Object getLatestMeasurementStringRecord(String stationtype, String id, BDPRole role) {
		Date date = null;
		EntityManager em = JPAUtil.createEntityManager();
		date = MeasurementStringHistory.findTimestampOfNewestRecordByStationId(em, stationtype, id, role);
		em.close();
		return date;
	}
	public List<StationDto> getStationsWithoutMunicipality(){
		List<StationDto> stationsDtos = new ArrayList<StationDto>();
		EntityManager em = JPAUtil.createEntityManager();
		List<Station> stations = Station.findStationsWithoutMunicipality(em);
		for (Station station : stations) {
			StationDto dto = station.convertToDto(station);
			String name = JPAUtil.getEntityNameByObject(station);
			dto.setStationType(name);
			stationsDtos.add(dto);
		}
		em.close();
		return stationsDtos;
	}
	public void patchStations(List<StationDto> stations) {
		EntityManager em = JPAUtil.createEntityManager();
		em.getTransaction().begin();
		for (StationDto dto:stations) {
			Station.patch(em,dto);
		}
		em.getTransaction().commit();
	}

	/*
	 * Updating sequences to be at the top of any serial id, because Hibernate does not
	 * do that automatically at startup. It just queries the max value of each sequence
	 * to get the next value. In addition, Hibernate does not generate a proper schema to
	 * auto increment on each insertion.
	 * XXX workaround to prevent unique key constraint violations during insertion: Should
	 * be changed ASAP because we need to hard-code DB specific sequence updates here!
	 */
	@EventListener(ContextRefreshedEvent.class)
	public void afterStartup() {
		EntityManager em = JPAUtil.createEntityManager();
		em.getTransaction().begin();
		em.createNativeQuery("select setval('station_seq', (select max(id) from station))").getSingleResult();
		em.createNativeQuery("select setval('type_seq', (select max(id) from type))").getSingleResult();
		em.createNativeQuery("select setval('measurement_id_seq', (select max(id) from measurement))")
				.getSingleResult();
		em.getTransaction().commit();
	}

}
