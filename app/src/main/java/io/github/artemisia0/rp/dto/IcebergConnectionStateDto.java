package io.github.artemisia0.rp.dto;

public class IcebergConnectionStateDto {

    private IcebergConnectionDto current;
    private IcebergConnectionDto defaults;

    public IcebergConnectionStateDto() {
    }

    public IcebergConnectionStateDto(IcebergConnectionDto current, IcebergConnectionDto defaults) {
        this.current = current;
        this.defaults = defaults;
    }

    public IcebergConnectionDto getCurrent() {
        return current;
    }

    public void setCurrent(IcebergConnectionDto current) {
        this.current = current;
    }

    public IcebergConnectionDto getDefaults() {
        return defaults;
    }

    public void setDefaults(IcebergConnectionDto defaults) {
        this.defaults = defaults;
    }
}
