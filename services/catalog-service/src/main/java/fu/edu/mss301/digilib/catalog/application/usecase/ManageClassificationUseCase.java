package fu.edu.mss301.digilib.catalog.application.usecase;

import fu.edu.mss301.digilib.catalog.application.command.ClassificationCommand;
import fu.edu.mss301.digilib.catalog.domain.entity.Classification;
import fu.edu.mss301.digilib.catalog.domain.repository.ClassificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ManageClassificationUseCase {

    private final ClassificationRepository classificationRepository;

    public Classification create(ClassificationCommand command) {
        Classification classification = Classification.builder()
                .classificationSystem(command.getClassificationSystem())
                .classificationName(command.getClassificationName())
                .classificationCode(command.getClassificationCode())
                .build();

        return classificationRepository.saveClassification(classification);
    }

    public Classification update(ClassificationCommand command) {
        Classification classification = findById(command.getClassificationId());
        classification.setClassificationSystem(command.getClassificationSystem());
        classification.setClassificationName(command.getClassificationName());
        classification.setClassificationCode(command.getClassificationCode());
        return classificationRepository.saveClassification(classification);
    }

    public void delete(ClassificationCommand command) {
        classificationRepository.deleteClassificationById(command.getClassificationId());
    }

    @Transactional(readOnly = true)
    public Page<Classification> findAll(Pageable pageable) {
        return classificationRepository.findAllClassifications(pageable);
    }

    @Transactional(readOnly = true)
    public Classification findById(Long classificationId) {
        return classificationRepository.findClassificationById(classificationId)
                .orElseThrow(() -> new IllegalArgumentException("Classification not found"));
    }

    @Transactional(readOnly = true)
    public Page<Classification> search(ClassificationCommand command, Pageable pageable) {
        return classificationRepository.searchClassifications(command.getKeyword(), pageable);
    }
}
