package it.bz.idm.bdp.dal.carpooling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import it.bz.idm.bdp.dal.MeasurementStation;
import it.bz.idm.bdp.dal.Station;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.carpooling.CarpoolingHubDto;
import it.bz.idm.bdp.dto.carpooling.LocationTranslationDto;

@Entity
public class Carpoolinghub extends MeasurementStation{

	
	@Override
	public List<StationDto> convertToDtos(EntityManager em, List<Station> resultList) {
		List<StationDto> dtos = new ArrayList<StationDto>();
		for (Station station: resultList){
			Double x = null,y = null;
			if (station.getPointprojection() != null){
				y = station.getPointprojection().getY();
				x = station.getPointprojection().getX();
			}
			CarpoolinghubBasicData basic = (CarpoolinghubBasicData) new CarpoolinghubBasicData().findByStation(em, station);
			if (basic == null)
				continue;
			Map<String,LocationTranslationDto> translationDtos = new HashMap<String,LocationTranslationDto>();
			for (Map.Entry<Locale, Translation> entry: basic.getI18n().entrySet()){
				if (entry.getValue() instanceof HubTranslation){
					HubTranslation translation = (HubTranslation) entry.getValue();
					translationDtos.put(entry.getKey().toLanguageTag(), new LocationTranslationDto(translation.getName(),translation.getAddress(),translation.getCity()));
				}
				
			}
			CarpoolingHubDto dto = new CarpoolingHubDto(station.getStationcode(),station.getName(),y,x,translationDtos);
			dto.setMunicipality(station.getMunicipality());
			dtos.add(dto);
		}
		return dtos;
	}
	
	@Override
	public void sync(EntityManager em, Station station, StationDto dto) {
		if (dto instanceof CarpoolingHubDto){
			CarpoolingHubDto carpoolingHubDto = (CarpoolingHubDto) dto;
			CarpoolinghubBasicData basic = (CarpoolinghubBasicData) new CarpoolinghubBasicData().findByStation(em, station);
			if (basic == null)
				basic = new CarpoolinghubBasicData();
			basic.setStation(station);
			for (Map.Entry<String, LocationTranslationDto> entry : carpoolingHubDto.getI18n().entrySet()){
				HubTranslation hubTranslation = (HubTranslation) basic.getI18n().get(Locale.forLanguageTag(entry.getKey()));
				if (hubTranslation == null){
					hubTranslation = new HubTranslation();
					basic.getI18n().put(Locale.forLanguageTag(entry.getKey()), hubTranslation);
				}
				hubTranslation.setName(entry.getValue().getName());
				hubTranslation.setAddress(entry.getValue().getAddress());
				hubTranslation.setCity(entry.getValue().getCity());
			}
			em.merge(basic);
		}
	}

}
