package no.rutebanken.tiamat.rest.example;

import no.rutebanken.tiamat.model.example.AnotherExampleEntity;
import no.rutebanken.tiamat.model.example.ExampleReferenceEntity;
import no.rutebanken.tiamat.model.example.ExampleEntity;
import no.rutebanken.tiamat.repository.example.AnotherExampleEntityRepository;
import no.rutebanken.tiamat.repository.example.ExampleEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Produces("application/json")
@Path("/exampleentity")
public class ExampleResource {

	@Autowired
	private ExampleEntityRepository exampleEntityRepository;


	@Autowired
	private AnotherExampleEntityRepository anotherExampleEntityRepository;

	@GET
	@Path("create")
	public ExampleEntity getEntity() {

		ExampleEntity exampleEntity = new ExampleEntity();

		exampleEntity.setReferences(new ArrayList<>());
		exampleEntity.getReferences().add(new AnotherExampleEntity());
		exampleEntity.getReferences().add(new AnotherExampleEntity());

		ExampleReferenceEntity exampleReferenceEntity = new ExampleReferenceEntity();
		exampleReferenceEntity.date = new Date();
		exampleReferenceEntity.anotherExampleEntity = new AnotherExampleEntity();
		anotherExampleEntityRepository.save(exampleReferenceEntity.anotherExampleEntity);

		List<ExampleReferenceEntity> referenceEntities = new ArrayList<>();
		referenceEntities.add(exampleReferenceEntity);

		exampleEntity.referenceObjects = referenceEntities;

		anotherExampleEntityRepository.save(exampleEntity.getReferences());
		exampleEntity.setName("example entity name");

		exampleEntityRepository.save(exampleEntity);
		return exampleEntity;
	}

	@GET
	public List<ExampleEntity> getEntities() {
		List<ExampleEntity> list = exampleEntityRepository.findAll();
		return list;
	}
}