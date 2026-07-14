package fu.edu.mss301.digilib.catalog.application.usecase;

import fu.edu.mss301.digilib.catalog.application.command.DigitalResourceCommand;
import fu.edu.mss301.digilib.catalog.domain.aggregate.BookAggregate;
import fu.edu.mss301.digilib.catalog.domain.entity.DigitalResource;
import fu.edu.mss301.digilib.catalog.domain.repository.BookRepository;
import fu.edu.mss301.digilib.catalog.domain.repository.DigitalResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ManageDigitalResourceUseCase {

    private final BookRepository bookRepository;
    private final DigitalResourceRepository digitalResourceRepository;

    public BookAggregate add(DigitalResourceCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        aggregate.addDigitalResource(
                command.getFileFormat(),
                command.getResourceUrl(),
                command.getAccessPermission(),
                command.getUserId()
        );
        return bookRepository.save(aggregate);
    }

    public BookAggregate update(DigitalResourceCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        aggregate.updateDigitalResource(
                command.getResourceId(),
                command.getFileFormat(),
                command.getResourceUrl(),
                command.getAccessPermission(),
                command.getUserId()
        );
        return bookRepository.save(aggregate);
    }

    public BookAggregate delete(DigitalResourceCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        aggregate.removeDigitalResource(command.getResourceId(), command.getUserId());
        return bookRepository.save(aggregate);
    }

    public BookAggregate restore(DigitalResourceCommand command) {
        BookAggregate aggregate = findAggregateIncludingDeleted(command.getBookId());
        aggregate.restoreDigitalResource(command.getResourceId(), command.getUserId());
        return bookRepository.save(aggregate);
    }

    public BookAggregate updateAccess(DigitalResourceCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        aggregate.updateDigitalResourceAccessPermission(
                command.getResourceId(),
                command.getAccessPermission(),
                command.getUserId()
        );
        return bookRepository.save(aggregate);
    }

    @Transactional(readOnly = true)
    public void ensureBookExists(Long bookId) {
        findAggregate(bookId);
    }

    @Transactional(readOnly = true)
    public DigitalResource access(DigitalResourceCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        return aggregate.accessDigitalResource(
                command.getResourceId(),
                command.getRequesterPermission()
        );
    }

    @Transactional(readOnly = true)
    public Page<DigitalResource> findAll(Pageable pageable) {
        return digitalResourceRepository.findAllDigitalResources(pageable);
    }

    @Transactional(readOnly = true)
    public DigitalResource findById(Long resourceId) {
        return digitalResourceRepository.findDigitalResourceById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Digital resource not found"));
    }

    @Transactional(readOnly = true)
    public Page<DigitalResource> findByBook(Long bookId, Pageable pageable) {
        return digitalResourceRepository.findDigitalResourcesByBookId(bookId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<DigitalResource> search(DigitalResourceCommand command, Pageable pageable) {
        return digitalResourceRepository.searchDigitalResources(command.getKeyword(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<DigitalResource> filter(DigitalResourceCommand command, Pageable pageable) {
        return digitalResourceRepository.filterDigitalResources(
                command.getFileFormat(),
                command.getAccessPermission(),
                pageable
        );
    }

    private BookAggregate findAggregate(Long bookId) {
        return bookRepository.findAggregateByBookId(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book aggregate not found"));
    }

    private BookAggregate findAggregateIncludingDeleted(Long bookId) {
        return bookRepository.findAggregateByBookIdIncludingDeleted(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book aggregate not found"));
    }
}
