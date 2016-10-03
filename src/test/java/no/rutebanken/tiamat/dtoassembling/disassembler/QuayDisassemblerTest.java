package no.rutebanken.tiamat.dtoassembling.disassembler;

import no.rutebanken.tiamat.dtoassembling.dto.QuayDto;
import no.rutebanken.tiamat.repository.QuayRepository;
import org.junit.Test;
import no.rutebanken.tiamat.model.Quay;
import no.rutebanken.tiamat.model.QuayTypeEnumeration;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuayDisassemblerTest {

    QuayRepository quayRepository = mock(QuayRepository.class);
    SimplePointDisassembler simplePointDisassembler = mock(SimplePointDisassembler.class);


    @Test
    public void disassembledQuayNotNull() {
        QuayDisassembler quayDisassembler = new QuayDisassembler(quayRepository, simplePointDisassembler);

        QuayDto quayDto = new QuayDto();

        Quay quay = quayDisassembler.disassemble(quayDto);

        assertThat(quay).isNotNull();
    }

    @Test
    public void disassembledQuayNName() {
        QuayDisassembler quayDisassembler = new QuayDisassembler(quayRepository, simplePointDisassembler);

        QuayDto quayDto = new QuayDto();
        quayDto.name = "name";
        Quay quay = quayDisassembler.disassemble(quayDto);

        assertThat(quay.getName().getValue()).isEqualTo(quayDto.name);
    }

    @Test
    public void disassembledQuayDescription() {
        QuayDisassembler quayDisassembler = new QuayDisassembler(quayRepository, simplePointDisassembler);

        QuayDto quayDto = new QuayDto();
        quayDto.description = "description";
        Quay quay = quayDisassembler.disassemble(quayDto);

        assertThat(quay.getDescription().getValue()).isEqualTo(quayDto.description);
    }


    @Test
    public void disassembledExistingQuay() {
        QuayDisassembler quayDisassembler = new QuayDisassembler(quayRepository, simplePointDisassembler);

        QuayDto quayDto = new QuayDto();
        quayDto.id = "12333";

        Quay quay = new Quay();
        quay.setId(Long.valueOf(quayDto.id));

        when(quayRepository.findOne(Long.valueOf(quayDto.id))).thenReturn(quay);

        Quay actualQuay = quayDisassembler.disassemble(quayDto);

        assertThat(actualQuay.getId().toString()).isEqualTo(quayDto.id);
    }

    @Test
    public void dissasembleQuayWithQuayType() {
        QuayDisassembler quayDisassembler = new QuayDisassembler(quayRepository, simplePointDisassembler);

        QuayDto quayDto = new QuayDto();
        quayDto.quayType = "vehicleLoadingPlace";

        Quay actualQuay = quayDisassembler.disassemble(quayDto);

        assertThat(actualQuay.getQuayType()).isEqualTo(QuayTypeEnumeration.VEHICLE_LOADING_PLACE);
    }

}