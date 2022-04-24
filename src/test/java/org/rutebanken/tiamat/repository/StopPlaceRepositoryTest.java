/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.repository;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.SiteRefStructure;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.ValidBetween;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class StopPlaceRepositoryTest extends TiamatIntegrationTest {
	private Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
	@Test
	public void findStopPlacesSortedCorrectly() {
		StopPlace stopPlaceOlder = new StopPlace();
		StopPlace stopPlaceNewer = new StopPlace();

		stopPlaceOlder.setChanged(Instant.ofEpochMilli(50));
		stopPlaceNewer.setChanged(Instant.ofEpochMilli(100));

		stopPlaceRepository.save(stopPlaceNewer);
		stopPlaceRepository.save(stopPlaceOlder);

		Pageable pageable = new PageRequest(0, 2);
		Page<StopPlace> page = stopPlaceRepository.findAllByOrderByChangedDesc(pageable);

		assertThat(page.getContent().get(0).getNetexId()).isEqualTo(stopPlaceNewer.getNetexId());
		assertThat(page.getContent().get(0).getChanged()).isEqualTo(stopPlaceNewer.getChanged());
		assertThat(page.getContent().get(1).getNetexId()).isEqualTo(stopPlaceOlder.getNetexId());
		assertThat(page.getContent().get(1).getChanged()).isEqualTo(stopPlaceOlder.getChanged());
	}

	@Test
	public void findStopPlacesByNameSortedCorrectly() {
		StopPlace stopPlaceOlder = new StopPlace();
		StopPlace stopPlaceNewer = new StopPlace();

		stopPlaceOlder.setChanged(Instant.ofEpochMilli(50));
		stopPlaceOlder.setName(new EmbeddableMultilingualString("it's older", "en"));

		stopPlaceNewer.setChanged(Instant.ofEpochMilli(100));
		stopPlaceNewer.setName(new EmbeddableMultilingualString("it's newer", "en"));

		stopPlaceRepository.save(stopPlaceNewer);
		stopPlaceRepository.save(stopPlaceOlder);

		Pageable pageable = new PageRequest(0, 2);
		Page<StopPlace> page = stopPlaceRepository.findByNameValueContainingIgnoreCaseOrderByChangedDesc("it", pageable);

		assertThat(page.getContent().get(0).getChanged()).isEqualTo(stopPlaceNewer.getChanged());
		assertThat(page.getContent().get(1).getChanged()).isEqualTo(stopPlaceOlder.getChanged());
	}


	@Test
	public void findByQuay() {
		StopPlace stopPlace = new StopPlace();
		Quay quay = new Quay();
		stopPlace.setQuays(Sets.newHashSet(quay));

		StopPlace savedStop = stopPlaceRepository.save(stopPlace);

		StopPlace foundStop = stopPlaceRepository.findByQuay(quay);
		Assert.assertEquals(savedStop, foundStop);
	}

	@Test
	public void findCurrentlyValidStopPlace() {
		StopPlace currentlyValid = new StopPlace();
		currentlyValid.setVersion(1L);
		currentlyValid.setValidBetween(new ValidBetween(now.minus(1, DAYS), now.plus(1, DAYS)));
		stopPlaceRepository.save(currentlyValid);

		StopPlaceSearch stopPlaceSearch = StopPlaceSearch.newStopPlaceSearchBuilder().setVersionValidity(ExportParams.VersionValidity.ALL).build();

		Page<StopPlace> results = stopPlaceRepository.findStopPlace(ExportParams.newExportParamsBuilder().setStopPlaceSearch(stopPlaceSearch).build());
		assertThat(results).hasSize(1);
		assertThat(results).contains(currentlyValid);
	}

	@Test
	public void findCurrentlyValidStopPlaceWithoutEndDate() {
		StopPlace currentlyValid = new StopPlace();
		currentlyValid.setVersion(1L);
		currentlyValid.setValidBetween(new ValidBetween(now.minus(1, DAYS)));
		stopPlaceRepository.save(currentlyValid);

		StopPlaceSearch stopPlaceSearch = StopPlaceSearch.newStopPlaceSearchBuilder().setVersionValidity(ExportParams.VersionValidity.ALL).build();

		Page<StopPlace> results = stopPlaceRepository.findStopPlace(ExportParams.newExportParamsBuilder().setStopPlaceSearch(stopPlaceSearch).build());
		assertThat(results).hasSize(1);
		assertThat(results).contains(currentlyValid);
	}


	@Test
	public void doNotFindExpiredStopPlaceVersion() {
		StopPlace expiredVersion = new StopPlace();
		expiredVersion.setVersion(1L);
		expiredVersion.setValidBetween(new ValidBetween(now.minus(2, DAYS), now.minus(1, DAYS)));
		stopPlaceRepository.save(expiredVersion);

		StopPlaceSearch stopPlaceSearch = StopPlaceSearch.newStopPlaceSearchBuilder().setVersionValidity(ExportParams.VersionValidity.CURRENT).build();

		Page<StopPlace> results = stopPlaceRepository.findStopPlace(ExportParams.newExportParamsBuilder().setStopPlaceSearch(stopPlaceSearch).build());
		assertThat(results).isEmpty();
	}

	@Test
	public void doNotFindFutureStopPlaceVersion() {
		StopPlace futureVersion = new StopPlace();
		futureVersion.setVersion(1L);
		futureVersion.setValidBetween(new ValidBetween(now.plus(1, DAYS), now.plus(2, DAYS)));
		stopPlaceRepository.save(futureVersion);

		StopPlaceSearch stopPlaceSearch = StopPlaceSearch.newStopPlaceSearchBuilder().setVersionValidity(ExportParams.VersionValidity.CURRENT).build();

		Page<StopPlace> results = stopPlaceRepository.findStopPlace(ExportParams.newExportParamsBuilder().setStopPlaceSearch(stopPlaceSearch).build());
		assertThat(results).isEmpty();
	}

	@Test
	public void doNotFindHistoricStopPlaceWithoutParentForCurrentAndFutureVersion() {
		StopPlace historicVersion = new StopPlace();
		historicVersion.setVersion(1L);
		historicVersion.setValidBetween(new ValidBetween(now.minus(3, DAYS), now.minus(2, DAYS)));
		stopPlaceRepository.save(historicVersion);

		StopPlaceSearch stopPlaceSearch = StopPlaceSearch.newStopPlaceSearchBuilder().setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE).build();

		Page<StopPlace> results = stopPlaceRepository.findStopPlace(ExportParams.newExportParamsBuilder().setStopPlaceSearch(stopPlaceSearch).build());
		assertThat(results).isEmpty();
	}

	@Test
	public void doNotFindHistoricStopPlaceWithParentForCurrentAndFutureVersion() {
		StopPlace historicParent = new StopPlace();
		historicParent.setVersion(1L);
		historicParent.setValidBetween(new ValidBetween(now.minus(3, DAYS), now.minus(2, DAYS)));
		stopPlaceRepository.save(historicParent);

		StopPlace historicChild=new StopPlace();
		historicChild.setVersion(1L);
		historicChild.setParentSiteRef(new SiteRefStructure(historicParent.getNetexId(), String.valueOf(historicParent.getVersion())));
		stopPlaceRepository.save(historicChild);

		StopPlaceSearch stopPlaceSearch = StopPlaceSearch.newStopPlaceSearchBuilder().setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE).build();

		Page<StopPlace> results = stopPlaceRepository.findStopPlace(ExportParams.newExportParamsBuilder().setStopPlaceSearch(stopPlaceSearch).build());
		assertThat(results).isEmpty();
	}

	@Test
	public void findFutureStopPlaceVersion() {
		StopPlace futureVersion = new StopPlace();
		futureVersion.setVersion(1L);
		futureVersion.setValidBetween(new ValidBetween(now.plus(1, DAYS), now.plus(2, DAYS)));
		stopPlaceRepository.save(futureVersion);

		StopPlaceSearch stopPlaceSearch = StopPlaceSearch.newStopPlaceSearchBuilder().setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE).build();

		Page<StopPlace> results = stopPlaceRepository.findStopPlace(ExportParams.newExportParamsBuilder().setStopPlaceSearch(stopPlaceSearch).build());
		assertThat(results)
				.hasSize(1)
				.contains(futureVersion);

	}


	@Test
	public void findFutureStopPlaceVersionWithoutEndDate() {
		StopPlace futureVersion = new StopPlace();
		futureVersion.setVersion(1L);
		futureVersion.setValidBetween(new ValidBetween(now.plus(1, DAYS)));
		stopPlaceRepository.save(futureVersion);

		StopPlaceSearch stopPlaceSearch = StopPlaceSearch.newStopPlaceSearchBuilder().setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE).build();

		Page<StopPlace> results = stopPlaceRepository.findStopPlace(ExportParams.newExportParamsBuilder().setStopPlaceSearch(stopPlaceSearch).build());
		assertThat(results)
				.hasSize(1)
				.contains(futureVersion);

	}
}